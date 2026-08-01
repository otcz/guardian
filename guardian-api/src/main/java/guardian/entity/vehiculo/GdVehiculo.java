package guardian.entity.vehiculo;

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
 * Vehiculo autorizado.
 *
 * <p>Pertenece a la <b>casa</b> y no a la persona: en la practica el carro
 * familiar lo maneja quien lo necesite ese dia, y obligar a que cada vehiculo
 * tenga un dueno unico haria que el guardia tuviera que denegar a la esposa por
 * conducir el carro que quedo registrado a nombre del esposo.</p>
 *
 * <p>La placa se guarda normalizada (mayusculas, sin espacios ni guiones) para
 * que la busqueda del guardia no falle por como se digito.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_VEHICULO",
        uniqueConstraints = @UniqueConstraint(
                // Placa unica en TODO el sistema: un mismo carro no puede estar
                // registrado en dos sedes ni en dos casas, o la bitacora no
                // podria decir por donde entro de verdad.
                name = "UK_VEHICULO_PLACA",
                columnNames = "PLACA"
        )
)
public class GdVehiculo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** Sede, desnormalizada desde la casa para poder filtrar sin un join. */
    @Column(name = "CONJUNTO_ID", nullable = false)
    private Long conjuntoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CASA_ID", nullable = false)
    private GdCasa casa;

    @Column(name = "PLACA", nullable = false, length = 10)
    private String placa;

    /** Codigo del grupo TIPO_VEHICULO en GD_PARAMETRO. */
    @Column(name = "TIPO", nullable = false, length = 20)
    private String tipo;

    @Column(name = "MARCA", length = 50)
    private String marca;

    @Column(name = "COLOR", length = 30)
    private String color;
}
