package guardian.dto.residente;

import lombok.Builder;
import lombok.Data;

/**
 * Miembro de la casa tal como lo ve el residente en "Mi hogar".
 */
@Data
@Builder
public class FamiliarResponse {

    private Long personaId;
    private String tipoDocumento;
    private String documento;
    private String nombreCompleto;
    private String parentesco;
    private String fotoUrl;
    private Integer edad;
    private String activo;

    /**
     * Llave del administrador. La pantalla del residente la necesita para
     * mostrar un candado en vez de un interruptor: sin esto, el titular ve un
     * boton que promete algo que el backend le va a negar.
     */
    private String bloqueado;
    private String motivoBloqueo;

    private boolean tieneCredencial;

    /**
     * Si puede entrar a la aplicacion. Sin cuenta la persona existe para la
     * porteria pero no puede abrir GUARDIAN, y eso hay que poder verlo desde
     * la lista: si no, el titular la da de alta, el familiar intenta entrar
     * con el PIN inicial y nadie entiende por que lo rechazan.
     */
    private boolean tieneCuenta;

    /**
     * Si su ultimo paso por la porteria fue una ENTRADA. Es lo que responde
     * "¿ya llego mi hijo?" sin tener que llamarlo.
     *
     * <p>Es lo que dice la bitacora, no una ubicacion: quien se fue caminando
     * sin escanear sigue figurando adentro hasta que vuelva a pasar.</p>
     */
    private boolean adentro;

    /** Marca al propio solicitante para que la UI no le ofrezca inactivarse. */
    private boolean esUsuarioActual;
}
