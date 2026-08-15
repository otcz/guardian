package guardian.entity.vehiculo;

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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Un titular pidiendo que le registren un vehiculo, a la espera de que la
 * administracion lo autorice.
 *
 * <p><b>Por que hay una aprobacion en el medio.</b> Registrar una placa es
 * darle paso al conjunto a un carro que el guardia no va a volver a discutir:
 * la pantalla se pone verde y la talanquera sube. Dejar eso a un formulario
 * del celular seria autoservicio de acceso vehicular.</p>
 *
 * <p><b>Por que es una tabla aparte y no un estado dentro de GD_VEHICULO.</b>
 * La placa es unica en todo el sistema. Si la solicitud viviera como una fila
 * de vehiculo "pendiente", un pedido rechazado dejaria la placa ocupada para
 * siempre y su dueno real no podria registrarla nunca. Aca la unicidad se
 * cobra al aprobar —que es donde el administrador puede ver el conflicto y
 * responderlo— y un rechazo no le quita la placa a nadie.</p>
 *
 * <p>La solicitud NO se borra al resolverse: queda con su estado y su motivo.
 * El dia que alguien pregunte por que ese carro entra al conjunto, la
 * respuesta tiene que existir y decir quien lo autorizo.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "GD_SOLICITUD_VEHICULO")
public class GdSolicitudVehiculo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** La casa a la que quedaria el vehiculo, no la persona: ver GdVehiculo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CASA_ID", nullable = false)
    private GdCasa casa;

    /** Quien lo pidio. Siempre el titular, pero se guarda quien fue. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SOLICITANTE_ID", nullable = false)
    private GdPersona solicitante;

    /** Ya normalizada: mayusculas, sin espacios ni guiones. */
    @Column(name = "PLACA", nullable = false, length = 10)
    private String placa;

    /** Codigo del grupo TIPO_VEHICULO en GD_PARAMETRO. */
    @Column(name = "TIPO", nullable = false, length = 20)
    private String tipo;

    @Column(name = "MARCA", length = 50)
    private String marca;

    @Column(name = "COLOR", length = 30)
    private String color;

    /**
     * Foto del vehiculo, ya subida por /api/fotos.
     *
     * <p>Viaja con la solicitud y no se pide despues de aprobar: el
     * administrador esta autorizando que un carro entre al conjunto, y "Mazda
     * gris" no es identificacion suficiente para eso. Al aprobar pasa tal cual
     * al vehiculo que nace.</p>
     */
    @Column(name = "FOTO_URL", length = 500)
    private String fotoUrl;

    /** PENDIENTE, APROBADA o RECHAZADA. */
    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    /** Por que se rechazo. Lo lee el residente, asi que se escribe para el. */
    @Column(name = "MOTIVO_RECHAZO", length = 200)
    private String motivoRechazo;
}
