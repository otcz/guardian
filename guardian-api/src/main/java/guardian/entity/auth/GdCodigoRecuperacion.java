package guardian.entity.auth;

import guardian.entity.base.BaseEntity;
import guardian.entity.persona.GdUsuario;
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
import javax.persistence.Version;

import java.util.Date;

/**
 * Codigo de un solo uso para recuperar la contrasena.
 *
 * <p><b>Se guarda HASHEADO.</b> Mientras vive es equivalente a la clave de la
 * cuenta: quien lo tenga puede cambiarla. Guardarlo en claro convertiria una
 * lectura de esta tabla —un respaldo mal guardado, un vistazo a la base— en el
 * control de todas las cuentas que estuvieran recuperando en ese momento.</p>
 *
 * <p>Las filas NO se borran al usarse. Que alguien haya pedido restablecer su
 * clave, cuando, y si lo logro, es justamente lo que hay que poder mirar
 * despues de una cuenta comprometida.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_CODIGO_RECUPERACION",
        indexes = @Index(name = "IX_RECUPERACION_USUARIO", columnList = "USUARIO_ID")
)
public class GdCodigoRecuperacion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private GdUsuario usuario;

    /** BCrypt del codigo de seis digitos. Nunca el codigo. */
    @Column(name = "CODIGO_HASH", nullable = false, length = 100)
    private String codigoHash;

    @Column(name = "FECHA_VENCIMIENTO", nullable = false)
    private Date fechaVencimiento;

    /** Momento en que se uso. Null = todavia no. */
    @Column(name = "FECHA_USO")
    private Date fechaUso;

    /**
     * Intentos fallidos contra ESTE codigo. Al llegar al tope se quema: sin
     * eso, seis digitos con diez minutos de vida son adivinables a fuerza de
     * peticiones.
     */
    @Column(name = "INTENTOS", nullable = false)
    private Integer intentos;

    /**
     * Dos pestanas escribiendo el mismo codigo a la vez podrian gastarlo dos
     * veces y cambiar la clave dos veces. El control optimista deja pasar una.
     */
    @Version
    @Column(name = "VERSION")
    private Long version;

    public boolean estaUsado() {
        return fechaUso != null;
    }

    public boolean estaVencido(Date momento) {
        return fechaVencimiento.before(momento);
    }

    /** Vive, no se ha usado, no se agoto y su duena no lo revoco. */
    public boolean sirve(Date momento, int intentosMaximos) {
        return puedeOperar()
                && !estaUsado()
                && !estaVencido(momento)
                && intentos < intentosMaximos;
    }
}
