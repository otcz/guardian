package guardian.entity.acceso;

import guardian.entity.base.BaseEntity;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
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
import javax.persistence.Version;
import java.util.Date;

/**
 * Invitacion con QR efimero — el caso opuesto a la credencial del residente.
 *
 * <p>El invitado NO es una persona del conjunto: no tiene cuenta, ni foto, ni
 * credencial permanente. Su control es distinto: vigencia acotada, usos
 * contados y el documento fisico que la porteria compara contra el que el
 * anfitrion declaro al crear la invitacion.</p>
 *
 * <p>El QR usa el prefijo GRDI y la misma firma HMAC del sistema: fabricar una
 * invitacion falsa exige la clave del servidor, igual que con las credenciales.
 * Revocar es {@code activo = 'N'} y surte efecto en el siguiente escaneo.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_INVITACION",
        indexes = {
                @Index(name = "IX_INVITACION_CASA", columnList = "CASA_ID"),
                @Index(name = "IX_INVITACION_CONJUNTO", columnList = "CONJUNTO_ID")
        }
)
public class GdInvitacion extends BaseEntity {

    /** Sin tope de entradas: la ventana de vigencia es todo el control. */
    public static final int SIN_LIMITE = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CONJUNTO_ID", nullable = false)
    private Long conjuntoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CASA_ID", nullable = false)
    private GdCasa casa;

    /** Residente que invita. A el se le rinde cuentas de este ingreso. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ANFITRION_ID", nullable = false)
    private GdPersona anfitrion;

    @Column(name = "NOMBRE_INVITADO", nullable = false, length = 150)
    private String nombreInvitado;

    /**
     * El invitado no tiene foto en el sistema, asi que el control en porteria
     * es este documento contra la cedula fisica que presenta.
     */
    @Column(name = "DOCUMENTO_INVITADO", nullable = false, length = 20)
    private String documentoInvitado;

    /** Placa normalizada si el invitado llega en vehiculo. Null = a pie. */
    @Column(name = "PLACA", length = 10)
    private String placa;

    @Column(name = "VIGENCIA_DESDE", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date vigenciaDesde;

    @Column(name = "VIGENCIA_HASTA", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date vigenciaHasta;

    /**
     * Tope de ENTRADAS. Ya no se pide al invitar: la visita la acota su
     * ventana de vigencia, no un contador — quien viene el sabado puede
     * entrar, salir a comprar y volver sin que nadie le lleve la cuenta.
     *
     * <p>La columna sigue existiendo, con {@link #SIN_LIMITE}, por dos
     * razones: es NOT NULL y el esquema se actualiza solo (nunca borra
     * columnas), y las invitaciones emitidas ANTES de este cambio se
     * repartieron prometiendo "una sola entrada". Esas se siguen respetando.</p>
     */
    @Column(name = "USOS_MAXIMOS", nullable = false)
    private Integer usosMaximos;

    /** Cuantas veces entro de verdad. La salida nunca cuenta. */
    @Column(name = "USOS_REALIZADOS", nullable = false)
    private Integer usosRealizados;

    /**
     * Bloqueo optimista. Dos porterias escaneando la misma invitacion de un
     * solo uso en el mismo instante leerian ambas usosRealizados=0 y ambas
     * dejarian entrar; con la version, la segunda escritura falla y el update
     * perdido se convierte en un 409 en vez de una entrada de mas. El default
     * en columna cubre las filas creadas antes de esta migracion.
     */
    @Version
    @Column(name = "VERSION", nullable = false, columnDefinition = "bigint default 0")
    private Long version;

    /** Identificador opaco que viaja en el QR y en el link publico. */
    @Column(name = "CODIGO_PUBLICO", nullable = false, unique = true, length = 64)
    private String codigoPublico;

    @Column(name = "FIRMA_HASH", nullable = false, length = 100)
    private String firmaHash;

    public boolean aunNoVigente(Date ahora) {
        return ahora.before(vigenciaDesde);
    }

    public boolean vencida(Date ahora) {
        return ahora.after(vigenciaHasta);
    }

    /**
     * Solo se agota lo que nacio con tope. Las invitaciones nuevas llevan
     * {@link #SIN_LIMITE} y nunca entran por aca.
     */
    public boolean agotada() {
        return usosMaximos != null && usosMaximos > SIN_LIMITE
                && usosRealizados >= usosMaximos;
    }
}
