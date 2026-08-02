package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.UsuarioRequest;
import guardian.dto.admin.UsuarioResponse;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdUsuarioRepository;
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
public class UsuarioServiceImpl implements UsuarioService {

    private final GdUsuarioRepository usuarioRepository;
    private final GdPersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ParametroService parametroService;
    private final guardian.security.EstadoUsuarioService estadoUsuarioService;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(UsuarioAutenticado ejecutor) {
        return usuarioRepository.listarPorConjunto(ejecutor.getConjuntoId())
                .stream()
                .filter(u -> !u.getId().equals(ejecutor.getUsuarioId()))
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    /**
     * SUPER_ADMIN no se asigna desde ningun panel.
     *
     * <p>No basta con no sembrarlo en el catalogo: el dia que alguien lo
     * agregue "para que aparezca en el combo", cualquier administrador de
     * cualquier sede se asciende con un PATCH y pasa a ver todas las demas.
     * Es un blindaje doble a proposito.</p>
     */
    private void exigirRolAsignable(String rol) {
        if (Codigos.ROL_SUPER_ADMIN.equals(rol)) {
            throw GuardianException.sinPermiso(MensajesGlobales.ROL_NO_ASIGNABLE);
        }
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request, UsuarioAutenticado ejecutor) {
        exigirRolAsignable(request.getRol());
        parametroService.exigirCodigoValido(Codigos.GRUPO_ROL, request.getRol());

        GdPersona persona = obtenerPersona(request.getPersonaId(), ejecutor.getConjuntoId());

        if (usuarioRepository.existsByPersonaId(persona.getId())) {
            throw GuardianException.conflicto("Esta persona ya tiene una cuenta.");
        }

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(persona);
        usuario.setRol(request.getRol());
        usuario.setClaveHash(passwordEncoder.encode(Codigos.CLAVE_INICIAL));
        usuario.setRequiereCambioClave(Codigos.SI);
        // Nace HABILITADA: el paso de activarla aparte se olvidaba, y el
        // resultado era una persona con su clave en la mano rebotando en el
        // login sin que nadie supiera por que.
        usuario.setActivo(Codigos.SI);
        usuario.setUsuarioCreador(ejecutor.getDocumento());

        GdUsuario guardado = usuarioRepository.save(usuario);
        log.info("[admin] usuario creado id={} personaId={} rol={} (habilitado)",
                guardado.getId(), persona.getId(), request.getRol());

        return mapear(guardado);
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarRol(Long id, String rol, UsuarioAutenticado ejecutor) {
        exigirRolAsignable(rol);
        parametroService.exigirCodigoValido(Codigos.GRUPO_ROL, rol);

        // Sin chequeo de autogestion: obtener() ya devuelve 404 sobre la
        // cuenta propia, asi que quitarse el rol es imposible por construccion.
        GdUsuario usuario = obtener(id, ejecutor);

        usuario.setRol(rol);
        usuario.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] usuario id={} rol={} por={}", id, rol, ejecutor.getDocumento());
        return mapear(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor) {
        GdUsuario usuario = obtener(id, ejecutor);
        usuario.setActivo(activo ? Codigos.SI : Codigos.NO);
        usuario.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] usuario id={} activo={} por={}", id, usuario.getActivo(),
                ejecutor.getDocumento());
        return mapear(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse restablecerClave(Long id, UsuarioAutenticado ejecutor) {
        GdUsuario usuario = obtener(id, ejecutor);

        usuario.setClaveHash(passwordEncoder.encode(Codigos.CLAVE_INICIAL));
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setUsuarioModificador(ejecutor.getDocumento());

        estadoUsuarioService.invalidar(usuario.getId());

        log.info("[admin] clave restablecida usuarioId={} por={}", id, ejecutor.getDocumento());
        return mapear(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse asignarClave(Long id, String claveNueva, UsuarioAutenticado ejecutor) {
        GdUsuario usuario = obtener(id, ejecutor);

        // La misma regla que en el cambio propio: si la clave asignada es el
        // documento, el "cambio obligatorio" seria decorativo — es justo la que
        // cualquiera adivina.
        if (claveNueva.equalsIgnoreCase(usuario.getPersona().getDocumento())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.CLAVE_IGUAL_AL_DOCUMENTO);
        }

        usuario.setClaveHash(passwordEncoder.encode(claveNueva));
        // Quien la asigno la conoce: mientras el dueno no la cambie, la cuenta
        // no es suya del todo.
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setUsuarioModificador(ejecutor.getDocumento());

        // Sin invalidar, la sesion abierta del dueno sigue con la autoridad
        // completa hasta que expire el TTL: le cambiamos la clave y el token
        // viejo seguiria trabajando como si nada.
        estadoUsuarioService.invalidar(usuario.getId());

        log.warn("[admin] clave ASIGNADA a usuarioId={} por={}", id, ejecutor.getDocumento());
        return mapear(usuarioRepository.save(usuario));
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resuelve una cuenta del panel.
     *
     * <p>La propia queda fuera con el MISMO 404 que la de otra sede. Antes se
     * dejaba pasar y se frenaba accion por accion; asi solo hay una regla, y
     * la que se olvide de repetir no abre un hueco.</p>
     */
    private GdUsuario obtener(Long id, UsuarioAutenticado ejecutor) {
        if (id.equals(ejecutor.getUsuarioId())) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }

        GdUsuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!usuario.getPersona().getConjunto().getId().equals(ejecutor.getConjuntoId())) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return usuario;
    }

    private GdPersona obtenerPersona(Long personaId, Long conjuntoId) {
        GdPersona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!persona.getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return persona;
    }

    private UsuarioResponse mapear(GdUsuario usuario) {
        GdPersona persona = usuario.getPersona();
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .personaId(persona.getId())
                .documento(persona.getDocumento())
                .nombreCompleto(persona.getNombreCompleto())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .bloqueado(usuario.getBloqueado())
                .motivoBloqueo(usuario.getMotivoBloqueo())
                .requiereCambioClave(usuario.debeCambiarClave())
                .fechaUltimoIngreso(usuario.getFechaUltimoIngreso())
                .build();
    }
}
