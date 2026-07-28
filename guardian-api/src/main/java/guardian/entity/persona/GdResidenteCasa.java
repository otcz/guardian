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
import javax.persistence.UniqueConstraint;

/**
 * Vinculo entre una persona y la casa donde vive.
 *
 * <p>Es una tabla aparte y no un campo en GdPersona porque una persona puede
 * vivir en dos unidades del mismo conjunto (padres e hijos con apartamentos
 * separados, un propietario que tambien arrienda) y porque el vinculo tiene
 * historia propia: cuando alguien se muda, el registro se desactiva pero no se
 * borra, y sus eventos de acceso pasados siguen teniendo sentido.</p>
 *
 * <p>El {@code parentesco} describe la relacion con la casa (TITULAR, ESPOSO,
 * ESPOSA, HIJO, OTRO). No es un rol de permisos: los permisos los da
 * {@link GdUsuario#getRol()}.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_RESIDENTE_CASA",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_RESIDENTE_PERSONA_CASA",
                columnNames = {"PERSONA_ID", "CASA_ID"}
        )
)
public class GdResidenteCasa extends BaseEntity {

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

    /** Codigo del grupo PARENTESCO en GD_PARAMETRO. */
    @Column(name = "PARENTESCO", nullable = false, length = 20)
    private String parentesco;
}
