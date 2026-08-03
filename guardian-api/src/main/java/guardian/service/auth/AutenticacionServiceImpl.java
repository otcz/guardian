package guardian.service.auth;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.auth.CambiarClaveRequest;
import guardian.dto.auth.LoginRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.auth.SesionResponse;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.EstadoUsuarioService;
import guardian.security.JwtService;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutenticacionServiceImpl implements AutenticacionService {

    private final GdUsuarioRepository usuarioRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdConjuntoRepository conjuntoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IntentosLoginService intentosLoginService;
    private final EstadoUsuarioService estadoUsuarioService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String documento = request.getDocumento().trim();

        intentosLoginService.exigirNoBloqueado(documento);

        GdUsuario usuario = usuarioRepository.buscarPorDocumento(documento)
                .orElseThrow(() -> {
                    // El mensaje no distingue "no existe" de "clave incorrecta":
                    // si lo hiciera, cualquiera podria averiguar que cedulas
                    // estan registradas en el conjunto probando numeros.
                    log.info("[auth] login fallido documento={} motivo=no_existe", documento);
                    intentosLoginService.registrarFallo(documento);
                    return GuardianException.noAutorizado(MensajesGlobales.CREDENCIALES_INVALIDAS);
                });

        if (!passwordEncoder.matches(request.getClave(), usuario.getClaveHash())) {
            log.info("[auth] login fallido documento={} motivo=clave_incorrecta", documento);
            intentosLoginService.registrarFallo(documento);
            throw GuardianException.noAutorizado(MensajesGlobales.CREDENCIALES_INVALIDAS);
        }

        // El chequeo de usuario activo va DESPUES de validar la clave. Al reves,
        // un desconocido podria distinguir cuentas deshabilitadas de inexistentes.
        // Las mismas tres condiciones que evalua el filtro en cada peticion:
        // usuario, persona y SEDE. Sin la sede aca, una sede desactivada
        // seguiria entregando tokens que el filtro rechaza despues — el
        // usuario entra "bien" y luego nada le funciona.
        if (!usuario.puedeOperar()
                || !usuario.getPersona().puedeOperar()
                || !usuario.getPersona().getConjunto().puedeOperar()) {
            log.info("[auth] login fallido documento={} motivo={}", documento,
                    usuario.estaBloqueado() ? "bloqueado" : "inactivo");
            throw GuardianException.noAutorizado(MensajesGlobales.USUARIO_INACTIVO);
        }

        intentosLoginService.limpiar(documento);
        usuario.setFechaUltimoIngreso(new Date());

        UsuarioAutenticado identidad = construirIdentidad(usuario);
        log.info("[auth] login ok personaId={} rol={}", identidad.getPersonaId(), identidad.getRol());

        return LoginResponse.builder()
                .token(jwtService.emitir(identidad))
                .usuario(construirSesion(usuario, identidad))
                .requiereCambioClave(usuario.debeCambiarClave())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse cambiarClave(UsuarioAutenticado autenticado, CambiarClaveRequest request) {
        GdUsuario usuario = usuarioRepository.findById(autenticado.getUsuarioId())
                .orElseThrow(() -> GuardianException.noAutorizado(MensajesGlobales.SESION_REQUERIDA));

        if (!passwordEncoder.matches(request.getClaveActual(), usuario.getClaveHash())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.CLAVE_ACTUAL_INCORRECTA);
        }

        // Sin esto el "cambio obligatorio" del primer ingreso seria decorativo:
        // bastaria con volver a escribir 0000 y la cuenta quedaria igual de
        // expuesta que antes.
        ValidadorPin.exigirValido(request.getClaveNueva(), autenticado.getDocumento());

        usuario.setClaveHash(passwordEncoder.encode(request.getClaveNueva()));
        usuario.setRequiereCambioClave(Codigos.NO);
        usuario.setUsuarioModificador(autenticado.getDocumento());

        // El cache de estado todavia dice "cambio pendiente"; sin invalidarlo,
        // el token nuevo naceria degradado hasta que expire el TTL.
        estadoUsuarioService.invalidar(usuario.getId());

        log.info("[auth] clave cambiada personaId={}", autenticado.getPersonaId());

        UsuarioAutenticado identidad = construirIdentidad(usuario);
        return LoginResponse.builder()
                .token(jwtService.emitir(identidad))
                .usuario(construirSesion(usuario, identidad))
                .requiereCambioClave(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SesionResponse sesionActual(UsuarioAutenticado autenticado) {
        GdUsuario usuario = usuarioRepository.findById(autenticado.getUsuarioId())
                .orElseThrow(() -> GuardianException.noAutorizado(MensajesGlobales.SESION_REQUERIDA));

        return construirSesion(usuario, autenticado);
    }

    private UsuarioAutenticado construirIdentidad(GdUsuario usuario) {
        GdPersona persona = usuario.getPersona();
        return new UsuarioAutenticado(
                usuario.getId(),
                persona.getId(),
                persona.getConjunto().getId(),
                persona.getDocumento(),
                persona.getNombreCompleto(),
                usuario.getRol(),
                usuario.debeCambiarClave());
    }

    private SesionResponse construirSesion(GdUsuario usuario, UsuarioAutenticado identidad) {
        GdPersona persona = usuario.getPersona();

        Optional<GdResidenteCasa> vinculo = residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI);

        // Mientras un super administrador esta suplantando, la sede que manda
        // es la del token, no la de su persona. Sin esto, refrescar la pagina
        // dentro de una sede devolveria "sin sede" y el banner desapareceria
        // justo cuando mas hace falta.
        GdConjunto sede = persona.getConjunto();
        Long sedeDelToken = identidad.getConjuntoId();
        if (sedeDelToken != null && !sedeDelToken.equals(sede.getId())) {
            sede = conjuntoRepository.findById(sedeDelToken).orElse(null);
        }

        // La fila de plataforma no es una sede: al super administrador recien
        // entrado hay que mostrarle "sin sede", no el nombre tecnico de la
        // fila que lo hospeda.
        boolean tieneSede = sede != null && !sede.esPlataforma();
        final GdConjunto resuelta = sede;

        return SesionResponse.builder()
                .usuarioId(usuario.getId())
                .personaId(persona.getId())
                .documento(persona.getDocumento())
                .nombreCompleto(persona.getNombreCompleto())
                .rol(usuario.getRol())
                .fotoUrl(persona.getFotoUrl())
                .casaIdentificador(vinculo
                        .map(v -> v.getCasa().getIdentificador())
                        .orElse(null))
                .sedeId(tieneSede ? resuelta.getId() : null)
                .sedeNombre(tieneSede ? resuelta.getNombre() : null)
                .sedeSuplantada(identidad.isSedeSuplantada())
                .build();
    }
}
