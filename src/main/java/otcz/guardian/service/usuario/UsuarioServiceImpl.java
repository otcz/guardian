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
import otcz.guardian.DTO.usuario.UsuarioRequestDTO;
import otcz.guardian.DTO.usuario.UsuarioResponseDTO;
import otcz.guardian.utils.DocumentoTipo;
import org.springframework.http.ResponseEntity;

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

    @Override
    public ResponseEntity<?> crearUsuarioDesdeDTO(UsuarioRequestDTO usuarioDTO) {
        try {
            // Validaciones de campos obligatorios
            if (usuarioDTO.getPassword() == null || usuarioDTO.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.ERROR_PASSWORD_OBLIGATORIO));
            }
            if (usuarioDTO.getCorreo() == null || usuarioDTO.getCorreo().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.ERROR_CORREO_OBLIGATORIO));
            }
            if (usuarioDTO.getNombreCompleto() == null || usuarioDTO.getNombreCompleto().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.ERROR_NOMBRE_OBLIGATORIO));
            }
            if (usuarioDTO.getDocumentoTipo() == null || usuarioDTO.getDocumentoTipo().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.ERROR_TIPO_DOCUMENTO_OBLIGATORIO));
            }
            if (usuarioDTO.getDocumentoNumero() == null || usuarioDTO.getDocumentoNumero().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.ERROR_NUMERO_DOCUMENTO_OBLIGATORIO));
            }
            if (usuarioDTO.getRol() == null) {
                return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.ERROR_ROL_OBLIGATORIO));
            }
            if (usuarioDTO.getEstado() == null) {
                return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.ERROR_ESTADO_OBLIGATORIO));
            }
            // Validación de unicidad
            if (usuarioRepository.findByCorreo(usuarioDTO.getCorreo()).isPresent()) {
                return ResponseEntity.badRequest().body(MensajeResponse.CORREO_YA_EXISTE);
            }
            if (usuarioRepository.findByDocumentoNumero(usuarioDTO.getDocumentoNumero()).isPresent()) {
                return ResponseEntity.badRequest().body(MensajeResponse.DOCUMENTO_YA_EXISTE);
            }
            // Mapeo DTO -> Entity modular
            UsuarioEntity usuarioEntity = mapToEntity(usuarioDTO);
            usuarioEntity.setPasswordHash(passwordEncoder.encode(usuarioDTO.getPassword()));
            usuarioEntity.setFechaRegistro(java.time.LocalDateTime.now());
            UsuarioEntity creado = usuarioRepository.save(usuarioEntity);
            UsuarioResponseDTO responseDTO = mapToResponseDTO(creado);
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(ex.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> actualizarUsuarioDesdeDTO(Long id, UsuarioRequestDTO usuarioDTO) {
        try {
            Optional<UsuarioEntity> existenteOpt = usuarioRepository.findById(id);
            if (!existenteOpt.isPresent()) {
                return ResponseEntity.badRequest().body(MensajeResponse.USUARIO_NO_ENCONTRADO);
            }
            UsuarioEntity existente = existenteOpt.get();
            if (usuarioDTO.getCorreo() != null) {
                existente.setCorreo(usuarioDTO.getCorreo());
            }
            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
                existente.setPasswordHash(passwordEncoder.encode(usuarioDTO.getPassword()));
            }
            if (usuarioDTO.getNombreCompleto() != null) {
                existente.setNombreCompleto(usuarioDTO.getNombreCompleto());
            }
            if (usuarioDTO.getDocumentoTipo() != null && !usuarioDTO.getDocumentoTipo().isEmpty()) {
                DocumentoTipo docTipo = DocumentoTipo.valueOf(usuarioDTO.getDocumentoTipo());
                existente.setDocumentoTipo(docTipo);
            }
            if (usuarioDTO.getDocumentoNumero() != null) {
                existente.setDocumentoNumero(usuarioDTO.getDocumentoNumero());
            }
            if (usuarioDTO.getRol() != null) {
                existente.setRol(usuarioDTO.getRol());
            }
            if (usuarioDTO.getEstado() != null) {
                existente.setEstado(usuarioDTO.getEstado());
            }
            if (usuarioDTO.getTelefono() != null) {
                existente.setTelefono(usuarioDTO.getTelefono());
            }
            UsuarioEntity actualizado = usuarioRepository.save(existente);
            UsuarioResponseDTO responseDTO = mapToResponseDTO(actualizado);
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(ex.getMessage()));
        }
    }

    @Override
    public UsuarioResponseDTO mapToResponseDTO(UsuarioEntity entity) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(entity.getId());
        dto.setNombreCompleto(entity.getNombreCompleto());
        dto.setCorreo(entity.getCorreo());
        dto.setTelefono(entity.getTelefono());
        dto.setDocumentoTipo(entity.getDocumentoTipo());
        dto.setDocumentoNumero(entity.getDocumentoNumero());
        dto.setRol(entity.getRol());
        dto.setEstado(entity.getEstado());
        dto.setFechaRegistro(entity.getFechaRegistro());
        dto.setUltimaConexion(entity.getUltimaConexion());
        return dto;
    }


    private UsuarioEntity mapToEntity(UsuarioRequestDTO dto) {
        UsuarioEntity usuarioEntity = new UsuarioEntity();
        usuarioEntity.setNombreCompleto(dto.getNombreCompleto());
        usuarioEntity.setCorreo(dto.getCorreo());
        usuarioEntity.setTelefono(dto.getTelefono());
        if (dto.getDocumentoTipo() != null && !dto.getDocumentoTipo().isEmpty()) {
            usuarioEntity.setDocumentoTipo(DocumentoTipo.valueOf(dto.getDocumentoTipo()));
        }
        usuarioEntity.setDocumentoNumero(dto.getDocumentoNumero());
        usuarioEntity.setRol(dto.getRol());
        usuarioEntity.setEstado(dto.getEstado());
        // El passwordHash y fechas se asignan en la lógica de negocio
        return usuarioEntity;
    }


}
