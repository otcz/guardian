package guardian.entity.persona;

import guardian.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Alguien pidiendo unirse a un hogar con el codigo de su titular, a la espera
 * de que la administracion lo apruebe.
 *
 * <p>Antes {@link guardian.service.residente.CodigoHogarService#registrar}
 * creaba la persona, su cuenta y su vinculo con la casa de una vez: el codigo
 * del titular alcanzaba para entrar. Eso dejaba el alta de un residente entero
 * —con acceso a la garita— en manos de un formulario publico sin sesion. Esta
 * solicitud es el paso que faltaba: los datos quedan aca hasta que un
 * administrador decide, y solo entonces se crea la persona de verdad.</p>
 *
 * <p>No se borra al resolverse, igual que {@link GdSolicitudCasa}: el dia que
 * alguien pregunte por que fulano quedo en esa casa, la respuesta tiene que
 * decir quien lo aprobo.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "GD_SOLICITUD_HOGAR")
public class GdSolicitudHogar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * El codigo con el que se pidio entrar. Vive la referencia y no solo la
     * casa: es por ahi que el titular sabe quien esta usando su codigo, y es
     * lo que impide que el mismo codigo acumule dos solicitudes a la vez.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CODIGO_ID", nullable = false)
    private GdCodigoHogar codigo;

    @Column(name = "TIPO_DOCUMENTO", length = 5)
    private String tipoDocumento;

    @Column(name = "DOCUMENTO", nullable = false, length = 20)
    private String documento;

    @Column(name = "NOMBRES", nullable = false, length = 100)
    private String nombres;

    @Column(name = "APELLIDOS", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "FECHA_NACIMIENTO")
    @Temporal(TemporalType.DATE)
    private Date fechaNacimiento;

    @Column(name = "TELEFONO", length = 30)
    private String telefono;

    @Column(name = "EMAIL", nullable = false, length = 120)
    private String email;

    /** Codigo del grupo PARENTESCO con el que pide entrar. Nunca TITULAR. */
    @Column(name = "PARENTESCO", nullable = false, length = 20)
    private String parentesco;

    /** PENDIENTE, APROBADA o RECHAZADA. */
    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    /** Por que se rechazo. Lo lee quien la mando, asi que se escribe para el. */
    @Column(name = "MOTIVO_RECHAZO", length = 200)
    private String motivoRechazo;
}
