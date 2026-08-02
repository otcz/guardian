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
 * Una SEDE: el conjunto residencial. Todas las tablas del dominio cuelgan de
 * aqui y el aislamiento entre sedes se sostiene sobre esta relacion.
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

    @Column(name = "DIRECCION", length = 200)
    private String direccion;

    // NIT y TELEFONO se quitaron: una sede se identifica por su nombre y se
    // ubica por su direccion, y nadie los consultaba. Las columnas siguen en
    // la base porque ddl-auto=update no borra nada; quedan vacias y sin uso.

    /**
     * Fila tecnica de la que cuelga el super administrador.
     *
     * <p>NO es una sede: queda fuera del listado, de los contadores y de todo
     * buscador. Existe porque el super administrador no pertenece a ninguna
     * sede real, pero {@code GD_PERSONA.CONJUNTO_ID} es NOT NULL en las bases
     * ya creadas y {@code ddl-auto=update} nunca relaja un NOT NULL: hacerlo
     * opcional exigiria un ALTER a mano. Con esta fila se cumple la regla de
     * negocio sin migracion manual y sin volver nullable una relacion de la
     * que dependen nueve validaciones de frontera.</p>
     */
    @Column(name = "ES_PLATAFORMA", nullable = false, length = 1,
            columnDefinition = "varchar(1) default 'N'")
    private String esPlataforma;

    public boolean esPlataforma() {
        return "S".equals(this.esPlataforma);
    }
}
