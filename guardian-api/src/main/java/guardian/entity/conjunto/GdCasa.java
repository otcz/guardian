package guardian.entity.conjunto;

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
import javax.persistence.UniqueConstraint;

/**
 * Unidad de vivienda. Cubre tanto casas como apartamentos: el conjunto decide
 * como nombrarlas con {@code torre} + {@code numero}.
 *
 * <p>Deshabilitar una casa deniega el ingreso de todos sus residentes de una
 * vez — es la palanca del administrador para casos de morosidad o sancion, sin
 * tener que tocar persona por persona.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_CASA",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_CASA_CONJUNTO_IDENTIFICADOR",
                columnNames = {"CONJUNTO_ID", "IDENTIFICADOR"}
        )
)
public class GdCasa extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CONJUNTO_ID", nullable = false)
    private GdConjunto conjunto;

    /**
     * Identificador visible de la unidad, unico dentro del conjunto.
     * Se arma como "TORRE-NUMERO" o solo el numero si no hay torres.
     * Es lo que el guardia ve en pantalla, asi que se guarda ya formateado.
     */
    @Column(name = "IDENTIFICADOR", nullable = false, length = 30)
    private String identificador;

    @Column(name = "TORRE", length = 20)
    private String torre;

    @Column(name = "NUMERO", nullable = false, length = 20)
    private String numero;

    /**
     * Cupos de parqueadero asignados.
     *
     * <p>HOY NO SE EXPONE. Se saco del formulario y de los DTO porque nadie lo
     * llenaba ni lo leia: pedirle un dato al administrador que el sistema no
     * usa solo alarga el alta. El campo y su columna se quedan —ddl-auto no
     * borra columnas— para el control de cupos de F4, que es cuando el dato
     * empieza a significar algo.</p>
     */
    @Column(name = "CUPOS_PARQUEADERO")
    private Integer cuposParqueadero;
}
