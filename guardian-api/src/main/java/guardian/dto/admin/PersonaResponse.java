package guardian.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class PersonaResponse {

    private Long id;
    private String tipoDocumento;
    private String documento;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private Date fechaNacimiento;
    private Integer edad;
    private String fotoUrl;
    private String telefono;
    private String email;
    private String activo;

    /** Llave del administrador: gana sobre "activo". */
    private String bloqueado;
    private String motivoBloqueo;

    private Long casaId;
    private String casaIdentificador;
    private String parentesco;

    /** Si ya tiene credencial QR activa. */
    private boolean tieneCredencial;

    /**
     * La cuenta de la aplicacion, si la tiene. Todo null cuando no la tiene —
     * que es el caso de la mayoria: los menores y las empleadas entran con QR
     * y nunca abren la aplicacion.
     *
     * <p>Viaja junto a la persona para que el panel pueda administrar las dos
     * cosas en un solo lugar. Sin esto la pantalla tendria que cruzar dos
     * listados a mano, que es justo lo que hacia que "Activa" y "cuenta sin
     * habilitar" parecieran contradecirse.</p>
     */
    private Long usuarioId;
    private String rol;
    private String usuarioActivo;
    private String usuarioBloqueado;
    private String usuarioMotivoBloqueo;
    private Date usuarioUltimoIngreso;
}
