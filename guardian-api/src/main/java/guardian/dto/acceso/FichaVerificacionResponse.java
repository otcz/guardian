package guardian.dto.acceso;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Lo que el guardia ve despues de escanear. Es la pantalla mas importante del
 * sistema.
 *
 * <p>El orden de los campos refleja el orden en que se leen bajo presion:
 * primero si pasa o no, despues la foto, y solo entonces el detalle. El guardia
 * esta de pie, con gente esperando y a veces de noche.</p>
 */
@Data
@Builder
public class FichaVerificacionResponse {

    /** Verde o rojo. Es lo primero que se lee, desde un metro de distancia. */
    private boolean permitido;

    /** Codigo del grupo MOTIVO_DENEGACION. Null cuando permitido = true. */
    private String motivoDenegacion;

    /** Mensaje ya redactado para mostrar en pantalla. */
    private String mensaje;

    // ── Identidad ────────────────────────────────────────────────────────────

    /**
     * La foto es el control real: el QR dice quien dice ser la persona, y esto
     * es lo que deja al guardia confirmarlo. En la interfaz va grande, no como
     * avatar decorativo.
     */
    private String fotoUrl;

    private String nombreCompleto;
    private String documento;
    private String casaIdentificador;
    private Integer edad;

    // ── Decision ─────────────────────────────────────────────────────────────

    /**
     * Sentido que el sistema infiere del ultimo movimiento: "E" o "S".
     * El guardia puede corregirlo, pero acierta casi siempre y le ahorra un
     * toque en hora pico.
     */
    private String sentidoSugerido;

    /** Vacio cuando la persona no tiene vehiculos registrados. */
    private List<VehiculoResumen> vehiculos;

    // ── Invitados ────────────────────────────────────────────────────────────

    /**
     * El invitado no tiene foto en el sistema: el control de la porteria es
     * comparar el documento fisico contra el declarado, y la interfaz lo dice.
     */
    private boolean esInvitado;

    /** Quien lo invito — el guardia puede confirmar con la casa si duda. */
    private String anfitrionNombre;

    /** Placa declarada en la invitacion, o null si viene a pie. */
    private String invitacionPlaca;

    /** Token que hay que devolver en el registro. Es el mismo payload escaneado. */
    private String payload;
}
