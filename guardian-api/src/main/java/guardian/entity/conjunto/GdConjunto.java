package guardian.entity.conjunto;

import guardian.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * El conjunto residencial. Hoy solo hay uno sembrado, pero todas las tablas del
 * dominio cuelgan de el.
 *
 * <p>Dejarlo desde el arranque es barato; agregarlo despues obligaria a migrar
 * cada tabla y cada query. Si el producto se vende a un segundo conjunto, el
 * aislamiento ya esta en el modelo y solo falta activar el filtro por tenant.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "GD_CONJUNTO")
public class GdConjunto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "NIT", length = 30)
    private String nit;

    @Column(name = "DIRECCION", length = 200)
    private String direccion;

    @Column(name = "TELEFONO", length = 30)
    private String telefono;
}
