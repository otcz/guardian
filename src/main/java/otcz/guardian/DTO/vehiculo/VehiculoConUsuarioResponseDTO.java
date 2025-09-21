package otcz.guardian.DTO.vehiculo;

import lombok.Data;
import otcz.guardian.utils.TipoVehiculo;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VehiculoConUsuarioResponseDTO {
    private Long id;
    private String placa;
    private TipoVehiculo tipo;
    private String color;
    private String marca;
    private String modelo;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private List<UsuarioSimpleDTO> usuarios;

    @Data
    public static class UsuarioSimpleDTO {
        private Long id;
        private String nombreCompleto;
        private String correo;
        private String telefono;
        private String rol;
        private String documentoIdentidad;
    }
}
