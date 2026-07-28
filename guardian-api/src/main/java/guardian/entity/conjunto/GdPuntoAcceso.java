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

/**
 * Punto por donde se entra o se sale: porteria peatonal, porteria vehicular,
 * puerta de servicio.
 *
 * <p>Existe desde F1 aunque el conjunto tenga una sola porteria, porque el
 * evento de acceso guarda por donde paso la persona. Sin ese dato, el dia que
 * el conjunto abra una segunda puerta el historico queda ambiguo y no hay como
 * reconstruirlo.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "GD_PUNTO_ACCESO")
public class GdPuntoAcceso extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CONJUNTO_ID", nullable = false)
    private GdConjunto conjunto;

    @Column(name = "NOMBRE", nullable = false, length = 80)
    private String nombre;

    /** Si admite ingreso de vehiculos. Una puerta peatonal no deberia registrar placas. */
    @Column(name = "PERMITE_VEHICULO", length = 1, nullable = false)
    private String permiteVehiculo;
}
