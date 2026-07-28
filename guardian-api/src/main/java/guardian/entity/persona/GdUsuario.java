package guardian.entity.persona;

import guardian.constant.Codigos;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Credenciales de acceso a la aplicacion. No toda persona tiene usuario: un
 * hijo menor puede tener QR para entrar al conjunto sin tener cuenta en la app.
 *
 * <p><b>Clave inicial = documento.</b> Es comodo de entregar pero significa que
 * cualquiera que conozca la cedula de un vecino podria tomar la cuenta antes que
 * el. La mitigacion es que el usuario nace con {@code activo = 'N'} y el
 * administrador lo habilita al entregar el acceso. Sin eso, la puerta de entrada
 * del sistema seria mas debil que la porteria fisica.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "GD_USUARIO")
public class GdUsuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERSONA_ID", nullable = false, unique = true)
    private GdPersona persona;

    /** Codigo del grupo ROL en GD_PARAMETRO: ADMIN, GUARDIA o RESIDENTE. */
    @Column(name = "ROL", nullable = false, length = 20)
    private String rol;

    /** Hash BCrypt. Nunca la clave en claro. */
    @Column(name = "CLAVE_HASH", nullable = false, length = 100)
    private String claveHash;

    @Column(name = "REQUIERE_CAMBIO_CLAVE", nullable = false, length = 1)
    private String requiereCambioClave;

    @Column(name = "FECHA_ULTIMO_INGRESO")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaUltimoIngreso;

    public boolean debeCambiarClave() {
        return Codigos.SI.equals(this.requiereCambioClave);
    }
}
