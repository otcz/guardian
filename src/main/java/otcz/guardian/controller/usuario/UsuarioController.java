package otcz.guardian.controller.usuario;


import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import otcz.guardian.utils.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import otcz.guardian.DTO.MensajeResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import otcz.guardian.DTO.usuario.UsuarioResponseDTO;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiEndpoints.Usuario.BASE)
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    private UsuarioResponseDTO mapToResponseDTO(UsuarioEntity entity) {
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioEntity usuarioEntity) {
        try {
            usuarioEntity.setPasswordHash(passwordEncoder.encode(usuarioEntity.getPasswordHash()));
            usuarioEntity.setFechaRegistro(LocalDateTime.now());
            UsuarioEntity creado = usuarioService.crearUsuario(usuarioEntity);
            return ResponseEntity.ok(mapToResponseDTO(creado));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(ex.getMessage()));
        }
    }

    @PutMapping(ApiEndpoints.Usuario.POR_ID)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioEntity usuarioEntity) {
        try {
            usuarioEntity.setId(id);
            UsuarioEntity actualizado = usuarioService.actualizarUsuario(usuarioEntity);
            return ResponseEntity.ok(mapToResponseDTO(actualizado));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(ex.getMessage()));
        }
    }

    @DeleteMapping(ApiEndpoints.Usuario.POR_ID)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok(new MensajeResponse("Usuario eliminado correctamente"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(ex.getMessage()));
        }
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ID)
    @PreAuthorize("hasAnyRole('ADMIN','GUARDIA')")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long id) {
        UsuarioResponseDTO dto = usuarioService.obtenerUsuarioPorId(id)
                .map(this::mapToResponseDTO)
                .orElse(null);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.ok(new MensajeResponse(MensajeResponse.USUARIO_NO_ENCONTRADO.getMensaje()));
        }
    }

    @GetMapping(ApiEndpoints.Usuario.POR_CORREO)
    @PreAuthorize("hasAnyRole('ADMIN','GUARDIA')")
    public ResponseEntity<?> obtenerUsuarioPorCorreo(@PathVariable String correo) {
        UsuarioResponseDTO dto = usuarioService.obtenerUsuarioPorCorreo(correo)
                .map(this::mapToResponseDTO)
                .orElse(null);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.ok(new MensajeResponse(MensajeResponse.USUARIO_NO_ENCONTRADO.getMensaje()));
        }
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ROL)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarUsuariosPorRol(@PathVariable Rol rol) {
        List<UsuarioEntity> usuarios = usuarioService.listarUsuariosPorRol(rol);
        List<UsuarioResponseDTO> dtos = usuarios.stream()
                .map(this::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ESTADO)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarUsuariosPorEstado(@PathVariable EstadoUsuario estado) {
        List<UsuarioEntity> usuarios = usuarioService.listarUsuariosPorEstado(estado);
        List<UsuarioResponseDTO> dtos = usuarios.stream()
                .map(this::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }


}
