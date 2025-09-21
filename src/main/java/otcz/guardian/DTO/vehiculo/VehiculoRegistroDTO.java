package otcz.guardian.DTO.vehiculo;

import lombok.Data;
import otcz.guardian.utils.TipoVehiculo;

import javax.validation.constraints.NotNull;

@Data
public class VehiculoRegistroDTO {
    private String placa;
    private TipoVehiculo tipo;
    private String color;
    private String marca;
    private String modelo;
    private Boolean activo;
    @NotNull(message = "El usuarioId es obligatorio")
    private Long usuarioId;
}
