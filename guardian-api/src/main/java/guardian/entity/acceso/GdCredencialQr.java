package guardian.entity.acceso;

import guardian.entity.base.BaseEntity;
import guardian.entity.persona.GdPersona;
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
 * Credencial QR de una persona.
 *
 * <p><b>El QR es permanente y firmado, no rotativo.</b> Un QR que rota cada 30
 * segundos obligaria al residente a tener datos moviles y la app abierta justo
 * al llegar a la porteria, que es donde peor senal hay. El riesgo de que alguien
 * fotografie el QR y lo reenvie se cubre en otro lado: el guardia ve la foto de
 * la persona en pantalla y compara.</p>
 *
 * <p>En la fila no se guarda el payload sino el <b>hash</b> del secreto. Si
 * alguien se lleva un volcado de la base, no puede fabricar QR validos con el.
 * La validacion recalcula el HMAC del codigo escaneado y lo compara contra esta
 * columna.</p>
 *
 * <p>Revocar es poner {@code activo = 'N'}: surte efecto en el siguiente
 * escaneo porque la firma siempre se valida contra la fila.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "GD_CREDENCIAL_QR")
public class GdCredencialQr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERSONA_ID", nullable = false)
    private GdPersona persona;

    /**
     * Identificador publico que viaja dentro del QR. Es un UUID opaco: no
     * revela el id de base ni ningun dato de la persona, asi que fotografiar el
     * QR no entrega informacion util por si solo.
     */
    @Column(name = "CODIGO_PUBLICO", nullable = false, unique = true, length = 64)
    private String codigoPublico;

    /** HMAC-SHA256 del codigo publico, en Base64 URL-safe. */
    @Column(name = "FIRMA_HASH", nullable = false, length = 100)
    private String firmaHash;

    /** Codigo del grupo TIPO_CREDENCIAL: PERMANENTE (residente) o TEMPORAL (invitado, F2). */
    @Column(name = "TIPO", nullable = false, length = 20)
    private String tipo;

    /** Null en credenciales permanentes. Los invitados de F2 si vencen. */
    @Column(name = "VIGENTE_HASTA")
    @Temporal(TemporalType.TIMESTAMP)
    private Date vigenteHasta;

    /** Null = sin limite. Los invitados de F2 traen un maximo de usos. */
    @Column(name = "USOS_MAXIMOS")
    private Integer usosMaximos;

    @Column(name = "USOS_REALIZADOS", nullable = false)
    private Integer usosRealizados;

    public boolean estaVencida(Date ahora) {
        return vigenteHasta != null && ahora.after(vigenteHasta);
    }

    public boolean agotoUsos() {
        return usosMaximos != null && usosRealizados != null && usosRealizados >= usosMaximos;
    }
}
