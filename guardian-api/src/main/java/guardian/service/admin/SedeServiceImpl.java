package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.auth.LoginResponse;
import guardian.dto.auth.SesionResponse;
import guardian.dto.sede.SedeRequest;
import guardian.dto.sede.SedeResponse;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.conjunto.GdPuntoAcceso;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdPuntoAccesoRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.JwtService;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SedeServiceImpl implements SedeService {

    private final GdConjuntoRepository conjuntoRepository;
    private final GdPuntoAccesoRepository puntoAccesoRepository;
    private final GdPersonaRepository personaRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final GdCasaRepository casaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public List<SedeResponse> listar() {
        return conjuntoRepository.findByEsPlataformaOrderByNombreAsc(Codigos.NO)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SedeResponse crear(SedeRequest request, UsuarioAutenticado ejecutor) {
        GdConjunto sede = crearConPorteria(request.getNombre(), request, ejecutor.getDocumento());
        log.info("[sede] creada id={} nombre={} por={}",
                sede.getId(), sede.getNombre(), ejecutor.getDocumento());
        return mapear(sede);
    }

    @Override
    @Transactional
    public GdConjunto crearConPorteria(String nombre, SedeRequest datos, String ejecutor) {
        String limpio = nombre.trim();

        conjuntoRepository.findFirstByNombreIgnoreCaseAndEsPlataforma(limpio, Codigos.NO)
                .ifPresent(otra -> {
                    throw GuardianException.conflicto(MensajesGlobales.SEDE_YA_REGISTRADA);
                });

        GdConjunto sede = new GdConjunto();
        sede.setNombre(limpio);
        sede.setEsPlataforma(Codigos.NO);
        sede.setActivo(Codigos.SI);
        sede.setBloqueado(Codigos.NO);
        sede.setUsuarioCreador(ejecutor);

        if (datos != null) {
            sede.setDireccion(datos.getDireccion());
        }

        GdConjunto guardada = conjuntoRepository.save(sede);

        // La porteria se crea CON la sede. Sin ella, la fabrica de eventos no
        // encuentra punto de acceso, lo deja nulo sin avisar, y meses despues
        // la bitacora no dice por donde entro nadie.
        GdPuntoAcceso porteria = new GdPuntoAcceso();
        porteria.setConjunto(guardada);
        porteria.setNombre("Porteria principal");
        // Arranca con la direccion de la sede: en un conjunto de una sola
        // porteria son la misma, y el administrador la corrige el dia que abra
        // la segunda.
        porteria.setDireccion(guardada.getDireccion());
        porteria.setPermiteVehiculo(Codigos.SI);
        porteria.setActivo(Codigos.SI);
        porteria.setBloqueado(Codigos.NO);
        porteria.setUsuarioCreador(ejecutor);
        puntoAccesoRepository.save(porteria);

        return guardada;
    }

    @Override
    @Transactional
    public SedeResponse actualizar(Long id, SedeRequest request, UsuarioAutenticado ejecutor) {
        GdConjunto sede = sedeReal(id);
        String limpio = request.getNombre().trim();

        conjuntoRepository.findFirstByNombreIgnoreCaseAndEsPlataforma(limpio, Codigos.NO)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw GuardianException.conflicto(MensajesGlobales.SEDE_YA_REGISTRADA);
                });

        sede.setNombre(limpio);
        sede.setDireccion(request.getDireccion());
        sede.setUsuarioModificador(ejecutor.getDocumento());

        return mapear(conjuntoRepository.save(sede));
    }

    @Override
    @Transactional
    public SedeResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor) {
        GdConjunto sede = sedeReal(id);
        sede.setActivo(activa ? Codigos.SI : Codigos.NO);
        sede.setUsuarioModificador(ejecutor.getDocumento());

        // Desactivar una sede expulsa a su gente: el estado de sesion se lee
        // fresco en cada peticion y ya contempla la sede.
        log.warn("[sede] id={} activa={} por={}", id, activa, ejecutor.getDocumento());
        return mapear(conjuntoRepository.save(sede));
    }

    @Override
    @Transactional
    public SedeResponse crearAdministrador(Long sedeId, PersonaRequest request,
                                           UsuarioAutenticado ejecutor) {
        GdConjunto sede = sedeReal(sedeId);

        if (usuarioRepository.existeRolEnConjunto(sedeId, Codigos.ROL_ADMIN)) {
            throw GuardianException.conflicto(MensajesGlobales.SEDE_YA_TIENE_ADMIN);
        }

        String documento = request.getDocumento().trim().toUpperCase();
        personaRepository.findByDocumento(documento).ifPresent(otra -> {
            throw GuardianException.conflicto(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);
        });

        GdPersona persona = new GdPersona();
        persona.setConjunto(sede);
        persona.setTipoDocumento(Codigos.TIPO_DOCUMENTO_CC);
        persona.setDocumento(documento);
        persona.setNombres(request.getNombres().trim());
        persona.setApellidos(request.getApellidos().trim());
        persona.setTelefono(request.getTelefono());
        persona.setEmail(guardian.util.CorreoUtil.normalizar(request.getEmail()));
        persona.setActivo(Codigos.SI);
        persona.setBloqueado(Codigos.NO);
        persona.setUsuarioCreador(ejecutor.getDocumento());

        GdPersona guardada = personaRepository.save(persona);

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(guardada);
        usuario.setRol(Codigos.ROL_ADMIN);
        usuario.setClaveHash(passwordEncoder.encode(Codigos.CLAVE_INICIAL));
        usuario.setRequiereCambioClave(Codigos.SI);
        // ACTIVO, al reves que toda otra cuenta del sistema: no hay nadie en
        // esa sede que pueda habilitarlo. Es la misma razon por la que el
        // administrador sembrado en el bootstrap tampoco nace inactivo.
        usuario.setActivo(Codigos.SI);
        usuario.setBloqueado(Codigos.NO);
        usuario.setUsuarioCreador(ejecutor.getDocumento());
        usuarioRepository.save(usuario);

        log.info("[sede] administrador creado sedeId={} documento={} por={}",
                sedeId, documento, ejecutor.getDocumento());

        return mapear(sede);
    }

    // ── Impersonacion ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public LoginResponse entrar(Long sedeId, UsuarioAutenticado ejecutor) {
        GdConjunto sede = sedeReal(sedeId);

        // Sin esto el token nace muerto: el filtro revisa la sede en CADA
        // peticion, asi que entrar a una sede apagada devolveria una sesion
        // valida que responde 401 a todo. Es peor que negar la entrada.
        if (!sede.puedeOperar()) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.SEDE_NO_OPERATIVA);
        }

        UsuarioAutenticado dentro = new UsuarioAutenticado(
                ejecutor.getUsuarioId(), ejecutor.getPersonaId(), sede.getId(),
                ejecutor.getDocumento(), ejecutor.getNombreCompleto(),
                ejecutor.getRol(), false, true);

        log.warn("[sede] super administrador {} ENTRA a la sede {} ({})",
                ejecutor.getDocumento(), sede.getId(), sede.getNombre());

        return sesionDe(dentro, sede);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse salir(UsuarioAutenticado ejecutor) {
        UsuarioAutenticado fuera = new UsuarioAutenticado(
                ejecutor.getUsuarioId(), ejecutor.getPersonaId(), null,
                ejecutor.getDocumento(), ejecutor.getNombreCompleto(),
                ejecutor.getRol(), false, false);

        log.info("[sede] super administrador {} sale de la sede", ejecutor.getDocumento());
        return sesionDe(fuera, null);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private LoginResponse sesionDe(UsuarioAutenticado identidad, GdConjunto sede) {
        return LoginResponse.builder()
                .token(jwtService.emitir(identidad))
                .requiereCambioClave(false)
                .usuario(SesionResponse.builder()
                        .usuarioId(identidad.getUsuarioId())
                        .personaId(identidad.getPersonaId())
                        .documento(identidad.getDocumento())
                        .nombreCompleto(identidad.getNombreCompleto())
                        .rol(identidad.getRol())
                        .sedeId(sede != null ? sede.getId() : null)
                        .sedeNombre(sede != null ? sede.getNombre() : null)
                        .sedeSuplantada(identidad.isSedeSuplantada())
                        .build())
                .build();
    }

    /** La fila de plataforma NO es una sede: no se lista, no se edita, no se entra. */
    private GdConjunto sedeReal(Long id) {
        return conjuntoRepository.findById(id)
                .filter(c -> !c.esPlataforma())
                .orElseThrow(() -> GuardianException.noEncontrado(
                        MensajesGlobales.SEDE_NO_ENCONTRADA));
    }

    private SedeResponse mapear(GdConjunto sede) {
        return SedeResponse.builder()
                .id(sede.getId())
                .nombre(sede.getNombre())
                .direccion(sede.getDireccion())
                .activo(sede.getActivo())
                .bloqueado(sede.getBloqueado())
                .casas(casaRepository.countByConjuntoId(sede.getId()))
                .personas(personaRepository.countByConjuntoId(sede.getId()))
                .usuarios(usuarioRepository.contarPorConjunto(sede.getId()))
                .tieneAdministrador(
                        usuarioRepository.existeRolEnConjunto(sede.getId(), Codigos.ROL_ADMIN))
                .build();
    }
}
