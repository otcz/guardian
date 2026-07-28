package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehiculoResponse {

    private Long id;
    private String placa;
    private String tipo;
    private String marca;
    private String color;
    private String activo;

    private Long casaId;
    private String casaIdentificador;
}
