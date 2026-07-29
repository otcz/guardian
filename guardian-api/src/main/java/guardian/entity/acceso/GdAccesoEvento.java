package guardian.entity.acceso;

import guardian.constant.Codigos;
import guardian.entity.base.BaseEntity;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdPuntoAcceso;
import guardian.entity.persona.GdPersona;
import guardian.entity.vehiculo.GdVehiculo;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Registro de un intento de acceso. Es el corazon del sistema: reemplaza la
 * minuta de papel y de aca salen todos los reportes.
 *
 * <p><b>Se escribe siempre, incluso cuando el acceso se deniega.</b> Una
 * credencial revocada que alguien sigue intentando usar es justo lo que un
 * administrador necesita ver, y si solo guardaramos los ingresos exitosos ese
 * patron seria invisible.</p>
 *
 * <p>Los datos de la persona se copian ({@code personaNombre}, {@code casaIdentificador},
 * {@code vehiculoPlaca}) ademas de guardar la FK. Es redundante a proposito: el
 * historico tiene que poder leerse tal como se vio esa noche, aunque despues la
 * persona se mude de casa, cambie de apellido o el vehiculo se transfiera. Sin
 * la copia, un reporte de hace seis meses mostraria la realidad de hoy.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_ACCESO_EVENTO",
        indexes = {
                @Index(name = "IX_ACCESO_PERSONA_FECHA", columnList = "PERSONA_ID,FECHA_EVENTO"),
                @Index(name = "IX_ACCESO_CONJUNTO_FECHA", columnList = "CONJUNTO_ID,FECHA_EVENTO"),
                @Index(name = "IX_ACCESO_CASA_FECHA", columnList = "CASA_ID,FECHA_EVENTO")
        }
)
public class GdAccesoEvento extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CONJUNTO_ID", nullable = false)
    private Long conjuntoId;

    @Column(name = "FECHA_EVENTO", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEvento;

    /** ENTRADA ("E") o SALIDA ("S"). Ver {@link Codigos}. */
    @Column(name = "SENTIDO", nullable = false, length = 1)
    private String sentido;

    /** PEATON o VEHICULO. */
    @Column(name = "MODO", nullable = false, length = 20)
    private String modo;

    /** PERMITIDO o DENEGADO. */
    @Column(name = "RESULTADO", nullable = false, length = 20)
    private String resultado;

    /** Codigo del grupo MOTIVO_DENEGACION. Null cuando el resultado es PERMITIDO. */
    @Column(name = "MOTIVO_DENEGACION", length = 40)
    private String motivoDenegacion;

    // ── Quien ────────────────────────────────────────────────────────────────

    /** Null si el QR no correspondia a nadie (codigo falsificado). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSONA_ID")
    private GdPersona persona;

    @Column(name = "PERSONA_NOMBRE", length = 200)
    private String personaNombre;

    @Column(name = "PERSONA_DOCUMENTO", length = 20)
    private String personaDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CASA_ID")
    private GdCasa casa;

    @Column(name = "CASA_IDENTIFICADOR", length = 30)
    private String casaIdentificador;

    // ── En que ───────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VEHICULO_ID")
    private GdVehiculo vehiculo;

    @Column(name = "VEHICULO_PLACA", length = 10)
    private String vehiculoPlaca;

    // ── Donde y quien atendio ────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PUNTO_ACCESO_ID")
    private GdPuntoAcceso puntoAcceso;

    /** Guardia que atendio. Es la trazabilidad de la porteria. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GUARDIA_PERSONA_ID")
    private GdPersona guardia;

    @Column(name = "GUARDIA_NOMBRE", length = 200)
    private String guardiaNombre;

    /** Credencial usada. Se guarda para poder rastrear un QR comprometido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREDENCIAL_ID")
    private GdCredencialQr credencial;

    /** Presente cuando quien cruzo fue un INVITADO; persona queda en null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVITACION_ID")
    private GdInvitacion invitacion;

    public boolean fuePermitido() {
        return Codigos.RESULTADO_PERMITIDO.equals(this.resultado);
    }
}
