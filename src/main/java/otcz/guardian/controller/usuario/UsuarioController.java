package otcz.guardian.controller.usuario;

import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import otcz.guardian.utils.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import otcz.guardian.DTO.MensajeResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import otcz.guardian.DTO.usuario.UsuarioResponseDTO;
import otcz.guardian.DTO.usuario.UsuarioRequestDTO;
import otcz.guardian.DTO.vehiculo.VehiculoAsignarUsuarioRequestDTO;
import otcz.guardian.service.vehiculo.VehiculoService;

import java.util.List;

@RestController
@RequestMapping(ApiEndpoints.Usuario.BASE)
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final VehiculoService vehiculoService;

    public UsuarioController(UsuarioService usuarioService, VehiculoService vehiculoService) {
        this.usuarioService = usuarioService;
        this.vehiculoService = vehiculoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioRequestDTO usuarioDTO) {
        return usuarioService.crearUsuarioDesdeDTO(usuarioDTO);
    }

    @PutMapping(ApiEndpoints.Usuario.POR_ID)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioRequestDTO usuarioDTO) {
        return usuarioService.actualizarUsuarioDesdeDTO(id, usuarioDTO);
    }

    @DeleteMapping(ApiEndpoints.Usuario.POR_ID)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok(MensajeResponse.USUARIO_ELIMINADO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(ex.getMessage()));
        }
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ID)
    @PreAuthorize("hasAnyRole('ADMIN','GUARDIA')")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long id) {
        return usuarioService.obtenerUsuarioPorId(id)
                .map(usuarioService::mapToResponseDTO)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(new MensajeResponse(MensajeResponse.USUARIO_NO_ENCONTRADO.getMensaje())));
    }

    @GetMapping(ApiEndpoints.Usuario.POR_CORREO)
    @PreAuthorize("hasAnyRole('ADMIN','GUARDIA')")
    public ResponseEntity<?> obtenerUsuarioPorCorreo(@PathVariable String correo) {
        return usuarioService.obtenerUsuarioPorCorreo(correo)
                .map(usuarioService::mapToResponseDTO)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(new MensajeResponse(MensajeResponse.USUARIO_NO_ENCONTRADO.getMensaje())));
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ROL)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarUsuariosPorRol(@PathVariable Rol rol) {
        List<UsuarioResponseDTO> dtos = usuarioService.listarUsuariosPorRol(rol)
                .stream()
                .map(usuarioService::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ESTADO)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarUsuariosPorEstado(@PathVariable EstadoUsuario estado) {
        List<UsuarioResponseDTO> dtos = usuarioService.listarUsuariosPorEstado(estado)
                .stream()
                .map(usuarioService::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(ApiEndpoints.Usuario.POR_DOCUMENTO)
    @PreAuthorize("hasAnyRole('ADMIN','GUARDIA')")
    public ResponseEntity<?> obtenerUsuarioPorDocumento(@PathVariable String documentoNumero) {
        return usuarioService.obtenerUsuarioPorDocumento(documentoNumero)
                .map(usuarioService::mapToResponseDTO)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(new MensajeResponse(MensajeResponse.USUARIO_NO_ENCONTRADO.getMensaje())));
    }

    @PostMapping(ApiEndpoints.Usuario.ASIGNAR_VEHICULO)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> asignarVehiculoAUsuario(@PathVariable Long usuarioId, @RequestBody VehiculoAsignarUsuarioRequestDTO request) {
        try {
            vehiculoService.asignarUsuario(request.getVehiculoId(), usuarioId);
            return ResponseEntity.ok(MensajeResponse.VEHICULO_ASIGNADO_A_USUARIO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(ex.getMessage()));
        }
    }

    @GetMapping(ApiEndpoints.Usuario.POR_CASA)
    @PreAuthorize("hasAnyRole('ADMIN','GUARDIA')")
    public ResponseEntity<?> listarUsuariosPorCasa(@PathVariable String casa) {
        List<UsuarioResponseDTO> dtos = usuarioService.listarUsuariosPorCasa(casa)
                .stream()
                .map(usuarioService::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }


}
