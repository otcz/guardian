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

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Long conjuntoId) {
        return usuarioRepository.findAll()
                .stream()
                .filter(u -> u.getPersona().getConjunto().getId().equals(conjuntoId))
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request, UsuarioAutenticado ejecutor) {
        parametroService.exigirCodigoValido(Codigos.GRUPO_ROL, request.getRol());

        GdPersona persona = obtenerPersona(request.getPersonaId(), ejecutor.getConjuntoId());

        if (usuarioRepository.existsByPersonaId(persona.getId())) {
            throw GuardianException.conflicto("Esta persona ya tiene una cuenta.");
        }

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(persona);
        usuario.setRol(request.getRol());
        usuario.setClaveHash(passwordEncoder.encode(persona.getDocumento()));
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setActivo(Codigos.NO);
        usuario.setUsuarioCreador(ejecutor.getDocumento());

        GdUsuario guardado = usuarioRepository.save(usuario);
        log.info("[admin] usuario creado id={} personaId={} rol={} (inactivo)",
                guardado.getId(), persona.getId(), request.getRol());

        return mapear(guardado);
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarRol(Long id, String rol, UsuarioAutenticado ejecutor) {
        parametroService.exigirCodigoValido(Codigos.GRUPO_ROL, rol);

        GdUsuario usuario = obtener(id, ejecutor.getConjuntoId());
        impedirAutogestion(usuario, ejecutor, "cambiar tu propio rol");

        usuario.setRol(rol);
        usuario.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] usuario id={} rol={} por={}", id, rol, ejecutor.getDocumento());
        return mapear(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor) {
        GdUsuario usuario = obtener(id, ejecutor.getConjuntoId());
        impedirAutogestion(usuario, ejecutor, "desactivar tu propia cuenta");

        usuario.setActivo(activo ? Codigos.SI : Codigos.NO);
        usuario.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] usuario id={} activo={} por={}", id, usuario.getActivo(),
                ejecutor.getDocumento());
        return mapear(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse restablecerClave(Long id, UsuarioAutenticado ejecutor) {
        GdUsuario usuario = obtener(id, ejecutor.getConjuntoId());

        usuario.setClaveHash(passwordEncoder.encode(usuario.getPersona().getDocumento()));
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] clave restablecida usuarioId={} por={}", id, ejecutor.getDocumento());
        return mapear(usuarioRepository.save(usuario));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdUsuario obtener(Long id, Long conjuntoId) {
        GdUsuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!usuario.getPersona().getConjunto().getId().equals(conjuntoId)) {
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

    /**
     * Evita que un administrador se deje a si mismo fuera. Si el unico admin del
     * conjunto se quita el rol o se desactiva, no queda nadie que pueda
     * devolverselo y hay que entrar a la base a mano.
     */
    private void impedirAutogestion(GdUsuario usuario, UsuarioAutenticado ejecutor, String accion) {
        if (usuario.getId().equals(ejecutor.getUsuarioId())) {
            throw GuardianException.solicitudInvalida("No puedes " + accion + ".");
        }
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
                .requiereCambioClave(usuario.debeCambiarClave())
                .fechaUltimoIngreso(usuario.getFechaUltimoIngreso())
                .build();
    }
}
