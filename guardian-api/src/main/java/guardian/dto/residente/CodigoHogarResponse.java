package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** El codigo que el titular comparte para que alguien se una a su hogar. */
@Data
@Builder
public class CodigoHogarResponse {

    private String codigo;
    private Date vigenciaHasta;

    /** Sin usar y dentro de su vigencia. Solo entonces vale la pena compartirlo. */
    private boolean vigente;

    /** Quien lo uso, si ya se uso. Null mientras siga esperando. */
    private String usadoPor;
}
