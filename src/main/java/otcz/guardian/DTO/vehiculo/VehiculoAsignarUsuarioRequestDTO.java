package otcz.guardian.DTO.vehiculo;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class VehiculoAsignarUsuarioRequestDTO {
    @NotNull
    private Long vehiculoId;
}

