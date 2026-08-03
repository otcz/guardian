package guardian.entity.conjunto;

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
import javax.persistence.UniqueConstraint;

/**
 * Que guardias trabajan en que porteria.
 *
 * <p>Tabla aparte y no un campo en la persona porque la relacion es de muchos a
 * muchos por los dos lados: una porteria tiene varios guardias —son turnos— y
 * un guardia puede cubrir mas de una puerta.</p>
 *
 * <p>La asignacion es el valor POR DEFECTO de lo que la tablet propone al
 * guardia, <b>no un candado</b>. Un guardia puede operar una porteria que no es
 * la suya y queda registrado: impedirselo dejaria gente esperando en la puerta
 * a las dos de la manana, y la bitacora ya permite ver quien opero donde, que es
 * el lugar correcto para detectarlo.</p>
 *
 * <p>Se apunta a la PERSONA y no a la cuenta: la persona sobrevive a que le
 * cambien el rol o le recreen el usuario, y el evento de acceso tambien guarda
 * la persona del guardia. Quitar a alguien desactiva la fila, no la borra —
 * saber quien estuvo asignado el dia de un incidente es justo lo que se va a
 * preguntar despues.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_GUARDIA_PORTERIA",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_GUARDIA_PORTERIA",
                columnNames = {"PERSONA_ID", "PUNTO_ACCESO_ID"}
        )
)
public class GdGuardiaPorteria extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERSONA_ID", nullable = false)
    private GdPersona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PUNTO_ACCESO_ID", nullable = false)
    private GdPuntoAcceso puntoAcceso;
}
