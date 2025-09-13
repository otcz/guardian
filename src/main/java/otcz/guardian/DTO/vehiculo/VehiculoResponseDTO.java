package otcz.guardian.DTO.vehiculo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import otcz.guardian.utils.TipoVehiculo;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoResponseDTO {
    private Long id;
    private String placa;
    private TipoVehiculo tipo;
    private String color;
    private String marca;
    private String modelo;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}

