package otcz.guardian.controller.vehiculo;

import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.DTO.vehiculo.VehiculoConUsuarioResponseDTO;
import otcz.guardian.DTO.vehiculo.VehiculoRegistroDTO;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.service.vehiculo.VehiculoService;
import otcz.guardian.utils.ApiEndpoints;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiEndpoints.Vehiculo.BASE)
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final UsuarioService usuarioService;

    public VehiculoController(VehiculoService vehiculoService, UsuarioService usuarioService) {
        this.vehiculoService = vehiculoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping(ApiEndpoints.Vehiculo.CREAR)
    public ResponseEntity<?> registrarVehiculo(@RequestBody VehiculoRegistroDTO vehiculoRegistroDTO) {
        vehiculoService.registrarVehiculo(vehiculoRegistroDTO);
        return ResponseEntity.ok(MensajeResponse.VEHICULO_CREADO);
    }

    @PutMapping(ApiEndpoints.Vehiculo.MODIFICAR)
    public ResponseEntity<?> actualizarVehiculo(@PathVariable Long id, @RequestBody VehiculoEntity vehiculoEntity) {
        if (!vehiculoService.obtenerPorId(id).isPresent()) {
            return ResponseEntity.status(404).body(MensajeResponse.VEHICULO_NO_ENCONTRADO);
        }
        vehiculoEntity.setId(id);
        vehiculoService.actualizarVehiculo(vehiculoEntity);
        return ResponseEntity.ok(MensajeResponse.VEHICULO_MODIFICADO);
    }

    // Eliminar vehículo por ID (ruta clara y sin ambigüedad)
    @DeleteMapping(ApiEndpoints.Vehiculo.ELIMINAR_POR_ID)
    public ResponseEntity<?> eliminarVehiculoPorId(@PathVariable Long id) {
        if (!vehiculoService.obtenerPorId(id).isPresent()) {
            return ResponseEntity.status(404).body(MensajeResponse.VEHICULO_NO_ENCONTRADO);
        }
        vehiculoService.eliminarVehiculo(id);
        return ResponseEntity.ok(MensajeResponse.VEHICULO_ELIMINADO);
    }

    // Eliminar vehículo por placa (ruta clara y sin ambigüedad)
    @DeleteMapping(ApiEndpoints.Vehiculo.ELIMINAR_POR_PLACA)
    public ResponseEntity<?> eliminarVehiculoPorPlaca(@PathVariable String placa) {
        try {
            vehiculoService.eliminarVehiculoPorPlaca(placa);
            return ResponseEntity.ok(MensajeResponse.VEHICULO_ELIMINADO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(new MensajeResponse(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new MensajeResponse(MensajeResponse.MENSAJE_GENERICO + " Detalle: " + ex.getMessage()));
        }
    }

    // Cambia la ruta para obtener por placa
    @GetMapping(ApiEndpoints.Vehiculo.POR_PLACA)
    public ResponseEntity<?> obtenerPorPlaca(@PathVariable String placa) {
        final ResponseEntity<?> notFound = ResponseEntity.status(404).body(MensajeResponse.VEHICULO_NO_ENCONTRADO);
        java.util.Optional<VehiculoEntity> vehiculoOpt = vehiculoService.obtenerPorPlaca(placa);
        return vehiculoOpt.isPresent() ? ResponseEntity.ok(vehiculoOpt.get()) : notFound;
    }

    @GetMapping(ApiEndpoints.Vehiculo.POR_USUARIO)
    public ResponseEntity<?> listarVehiculosPorUsuario(@PathVariable Long usuarioId) {
        final ResponseEntity<?> notFound = ResponseEntity.status(404).body(MensajeResponse.USUARIO_NO_ENCONTRADO);
        Optional<?> usuarioOpt = usuarioService.obtenerUsuarioPorId(usuarioId);
        if (usuarioOpt.isPresent()) {
            List<VehiculoEntity> lista = vehiculoService.listarVehiculosPorUsuario((UsuarioEntity) usuarioOpt.get());
            return ResponseEntity.ok(lista);
        } else {
            return notFound;
        }
    }

    @GetMapping(ApiEndpoints.Vehiculo.POR_TIPO)
    public ResponseEntity<?> listarVehiculosPorTipo(@PathVariable TipoVehiculo tipo) {
        List<VehiculoEntity> lista = vehiculoService.listarVehiculosPorTipo(tipo);
        return ResponseEntity.ok(lista);
    }

    @GetMapping(ApiEndpoints.Vehiculo.ACTIVOS_POR_USUARIO)
    public ResponseEntity<?> listarVehiculosActivosPorUsuario(@PathVariable Long usuarioId) {
        final ResponseEntity<?> notFound = ResponseEntity.status(404).body(MensajeResponse.USUARIO_NO_ENCONTRADO);
        Optional<?> usuarioOpt = usuarioService.obtenerUsuarioPorId(usuarioId);
        if (usuarioOpt.isPresent()) {
            List<VehiculoEntity> lista = vehiculoService.listarVehiculosActivosPorUsuario((UsuarioEntity) usuarioOpt.get());
            return ResponseEntity.ok(lista);
        } else {
            return notFound;
        }
    }

    @GetMapping()
    public ResponseEntity<List<VehiculoConUsuarioResponseDTO>> listarTodos() {
        List<VehiculoEntity> lista = vehiculoService.listarTodos();
        List<VehiculoConUsuarioResponseDTO> respuesta = lista.stream().map(vehiculo -> {
            VehiculoConUsuarioResponseDTO dto = new VehiculoConUsuarioResponseDTO();
            dto.setId(vehiculo.getId());
            dto.setPlaca(vehiculo.getPlaca());
            dto.setTipo(vehiculo.getTipo());
            dto.setColor(vehiculo.getColor());
            dto.setMarca(vehiculo.getMarca());
            dto.setModelo(vehiculo.getModelo());
            dto.setActivo(vehiculo.getActivo());
            dto.setFechaRegistro(vehiculo.getFechaRegistro());
            // Mapear usuarios desde la tabla intermedia
            List<VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO> usuariosDto = vehiculo.getVehiculoUsuarios().stream()
                .map(new java.util.function.Function<otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity, VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO>() {
                    @Override
                    public VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO apply(otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity rel) {
                        UsuarioEntity usuario = rel.getUsuario();
                        VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO usuarioDto = new VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO();
                        usuarioDto.setId(usuario.getId());
                        usuarioDto.setNombreCompleto(usuario.getNombreCompleto());
                        usuarioDto.setCorreo(usuario.getCorreo());
                        usuarioDto.setTelefono(usuario.getTelefono());
                        usuarioDto.setDocumentoIdentidad(usuario.getDocumentoNumero());
                        usuarioDto.setRol(usuario.getRol() != null ? usuario.getRol().name() : null);
                        return usuarioDto;
                    }
                })
                .collect(Collectors.toList());
            dto.setUsuarios(usuariosDto);
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/mis-vehiculos")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<List<VehiculoConUsuarioResponseDTO>> listarVehiculosDelUsuarioAutenticado(Authentication authentication) {
        String correo = authentication.getName();
        List<VehiculoConUsuarioResponseDTO> vehiculos = vehiculoService.listarVehiculosPorCorreoUsuario(correo);
        return ResponseEntity.ok(vehiculos);
    }

    /**
     * Asigna un usuario a un vehículo (tabla intermedia).
     * @param vehiculoId ID del vehículo
     * @param usuarioId ID del usuario
     */
    @PostMapping("/asignar-usuario")
    public ResponseEntity<?> asignarUsuarioAVehiculo(@RequestParam Long vehiculoId, @RequestParam Long usuarioId) {
        try {
            vehiculoService.asignarUsuarioAVehiculo(vehiculoId, usuarioId);
            return ResponseEntity.ok(MensajeResponse.USUARIO_ASIGNADO_VEHICULO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
