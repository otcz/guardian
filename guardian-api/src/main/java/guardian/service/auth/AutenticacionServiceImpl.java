package guardian.service.auth;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.auth.CambiarClaveRequest;
import guardian.dto.auth.LoginRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.auth.SesionResponse;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String documento = request.getDocumento().trim();

        GdUsuario usuario = usuarioRepository.buscarPorDocumento(documento)
                .orElseThrow(() -> {
                    // El mensaje no distingue "no existe" de "clave incorrecta":
                    // si lo hiciera, cualquiera podria averiguar que cedulas
                    // estan registradas en el conjunto probando numeros.
                    log.info("[auth] login fallido documento={} motivo=no_existe", documento);
                    return GuardianException.noAutorizado(MensajesGlobales.CREDENCIALES_INVALIDAS);
                });

        if (!passwordEncoder.matches(request.getClave(), usuario.getClaveHash())) {
            log.info("[auth] login fallido documento={} motivo=clave_incorrecta", documento);
            throw GuardianException.noAutorizado(MensajesGlobales.CREDENCIALES_INVALIDAS);
        }

        // El chequeo de usuario activo va DESPUES de validar la clave. Al reves,
        // un desconocido podria distinguir cuentas deshabilitadas de inexistentes.
        if (!usuario.estaActivo()) {
            log.info("[auth] login fallido documento={} motivo=inactivo", documento);
            throw GuardianException.noAutorizado(MensajesGlobales.USUARIO_INACTIVO);
        }

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
    public void cambiarClave(UsuarioAutenticado autenticado, CambiarClaveRequest request) {
        GdUsuario usuario = usuarioRepository.findById(autenticado.getUsuarioId())
                .orElseThrow(() -> GuardianException.noAutorizado(MensajesGlobales.SESION_REQUERIDA));

        if (!passwordEncoder.matches(request.getClaveActual(), usuario.getClaveHash())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.CLAVE_ACTUAL_INCORRECTA);
        }

        // Sin esta validacion el "cambio obligatorio" del primer ingreso seria
        // decorativo: bastaria con volver a escribir el documento y la cuenta
        // quedaria igual de expuesta que antes.
        if (request.getClaveNueva().equals(autenticado.getDocumento())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.CLAVE_IGUAL_AL_DOCUMENTO);
        }

        usuario.setClaveHash(passwordEncoder.encode(request.getClaveNueva()));
        usuario.setRequiereCambioClave(Codigos.NO);
        usuario.setUsuarioModificador(autenticado.getDocumento());

        log.info("[auth] clave cambiada personaId={}", autenticado.getPersonaId());
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
                usuario.getRol());
    }

    private SesionResponse construirSesion(GdUsuario usuario, UsuarioAutenticado identidad) {
        GdPersona persona = usuario.getPersona();

        Optional<GdResidenteCasa> vinculo = residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI);

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
                .build();
    }
}
