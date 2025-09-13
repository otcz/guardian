package otcz.guardian.service.usuario;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import otcz.guardian.entity.usuario.UsuarioDetails;
import otcz.guardian.DTO.MensajeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UsuarioEntity crearUsuario(UsuarioEntity usuarioEntity) {
        StringBuilder errores = new StringBuilder();
        if (usuarioEntity.getCorreo() == null || usuarioEntity.getCorreo().trim().isEmpty()) {
            errores.append(MensajeResponse.ERROR_CORREO_OBLIGATORIO + " ");
        }
        if (usuarioEntity.getPasswordHash() == null || usuarioEntity.getPasswordHash().trim().isEmpty()) {
            errores.append(MensajeResponse.ERROR_PASSWORD_OBLIGATORIO + " ");
        }
        if (usuarioEntity.getNombreCompleto() == null || usuarioEntity.getNombreCompleto().trim().isEmpty()) {
            errores.append(MensajeResponse.ERROR_NOMBRE_OBLIGATORIO + " ");
        }
        if (usuarioEntity.getDocumentoTipo() == null) {
            errores.append(MensajeResponse.ERROR_TIPO_DOCUMENTO_OBLIGATORIO + " ");
        }
        if (usuarioEntity.getDocumentoNumero() == null || usuarioEntity.getDocumentoNumero().trim().isEmpty()) {
            errores.append(MensajeResponse.ERROR_NUMERO_DOCUMENTO_OBLIGATORIO + " ");
        }
        if (usuarioEntity.getRol() == null) {
            errores.append(MensajeResponse.ERROR_ROL_OBLIGATORIO + " ");
        }
        if (usuarioEntity.getEstado() == null) {
            errores.append(MensajeResponse.ERROR_ESTADO_OBLIGATORIO + " ");
        }
        if (errores.length() > 0) {
            throw new IllegalArgumentException(errores.toString().trim());
        }
        if (usuarioRepository.findByCorreo(usuarioEntity.getCorreo()).isPresent()) {
            throw new IllegalArgumentException(MensajeResponse.CORREO_YA_EXISTE.getMensaje());
        }
        if (usuarioRepository.findByDocumentoNumero(usuarioEntity.getDocumentoNumero()).isPresent()) {
            throw new IllegalArgumentException(MensajeResponse.DOCUMENTO_YA_EXISTE.getMensaje());
        }
        return usuarioRepository.save(usuarioEntity);
    }

    @Override
    public UsuarioEntity actualizarUsuario(UsuarioEntity usuarioEntity) {
        Optional<UsuarioEntity> existenteOpt = usuarioRepository.findById(usuarioEntity.getId());
        if (!existenteOpt.isPresent()) {
            throw new IllegalArgumentException(MensajeResponse.USUARIO_NO_ENCONTRADO.getMensaje());
        }
        UsuarioEntity existente = existenteOpt.get();
        // Solo actualiza los campos que vienen en el body, nunca sobrescribas fechaRegistro ni ultimaConexion con null
        if (usuarioEntity.getCorreo() != null) {
            existente.setCorreo(usuarioEntity.getCorreo());
        }
        if (usuarioEntity.getPasswordHash() != null) {
            existente.setPasswordHash(passwordEncoder.encode(usuarioEntity.getPasswordHash()));
        }
        if (usuarioEntity.getNombreCompleto() != null) {
            existente.setNombreCompleto(usuarioEntity.getNombreCompleto());
        }
        if (usuarioEntity.getDocumentoTipo() != null) {
            existente.setDocumentoTipo(usuarioEntity.getDocumentoTipo());
        }
        if (usuarioEntity.getDocumentoNumero() != null) {
            existente.setDocumentoNumero(usuarioEntity.getDocumentoNumero());
        }
        if (usuarioEntity.getRol() != null) {
            existente.setRol(usuarioEntity.getRol());
        }
        if (usuarioEntity.getEstado() != null) {
            existente.setEstado(usuarioEntity.getEstado());
        }
        if (usuarioEntity.getTelefono() != null) {
            existente.setTelefono(usuarioEntity.getTelefono());
        }
        // fechaRegistro y ultimaConexion nunca se sobrescriben con null
        return usuarioRepository.save(existente);
    }

    @Override
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException(MensajeResponse.USUARIO_NO_ENCONTRADO.getMensaje());
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    public Optional<UsuarioEntity> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<UsuarioEntity> obtenerUsuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    @Override
    public Optional<UsuarioEntity> obtenerUsuarioPorDocumento(String documentoNumero) {
        return usuarioRepository.findByDocumentoNumero(documentoNumero);
    }

    @Override
    public List<UsuarioEntity> listarUsuariosPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol);
    }

    @Override
    public List<UsuarioEntity> listarUsuariosPorEstado(EstadoUsuario estado) {
        return usuarioRepository.findByEstado(estado);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreo(username)
                .map(UsuarioDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + username));
    }
}
