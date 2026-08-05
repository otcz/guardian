package guardian.entity.persona;

import guardian.entity.base.BaseEntity;
import guardian.entity.conjunto.GdCasa;
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
 * Un residente pidiendo entrar a una casa, a la espera de que la
 * administracion lo apruebe.
 *
 * <p><b>Por que hay una aprobacion en el medio.</b> Elegir casa NO puede ser
 * inmediato: quien entra a un hogar se queda con su contexto —sus vehiculos,
 * sus invitaciones, su familia— y si la casa no tiene titular, se convierte en
 * titular. En un sistema cuyo trabajo es controlar quien entra, dejar eso a un
 * clic seria autoservicio de acceso.</p>
 *
 * <p>La solicitud NO se borra al resolverse: queda con su estado y su motivo.
 * El dia que alguien pregunte por que fulano quedo en esa casa, la respuesta
 * tiene que existir y decir quien lo aprobo.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "GD_SOLICITUD_CASA")
public class GdSolicitudCasa extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERSONA_ID", nullable = false)
    private GdPersona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CASA_ID", nullable = false)
    private GdCasa casa;

    /** Codigo del grupo PARENTESCO con el que pide entrar. */
    @Column(name = "PARENTESCO", nullable = false, length = 20)
    private String parentesco;

    /** PENDIENTE, APROBADA o RECHAZADA. */
    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    /** Por que se rechazo. Lo lee el residente, asi que se escribe para el. */
    @Column(name = "MOTIVO_RECHAZO", length = 200)
    private String motivoRechazo;
}
