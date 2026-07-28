package guardian.entity.parametro;

import guardian.entity.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Catalogo parametrico. Una sola tabla para todos los grupos de valores que el
 * administrador puede ajustar sin esperar un deploy: parentescos, tipos de
 * vehiculo, motivos de denegacion, roles.
 *
 * <p>Lo que NO vive aca son los discriminadores estructurales — sentido del
 * acceso, modo, resultado — porque el codigo ramifica sobre ellos y agregar un
 * valor nuevo exigiria escribir logica nueva igual. Meterlos daria la ilusion de
 * que se pueden crear desde la interfaz. Esos estan en
 * {@link guardian.constant.Codigos}.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_PARAMETRO",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PARAMETRO_GRUPO_CODIGO",
                columnNames = {"GRUPO", "CODIGO"}
        ),
        indexes = @Index(name = "IX_PARAMETRO_GRUPO", columnList = "GRUPO")
)
public class GdParametro extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "GRUPO", nullable = false, length = 40)
    private String grupo;

    @Column(name = "CODIGO", nullable = false, length = 40)
    private String codigo;

    /** Texto que se muestra en la interfaz. */
    @Column(name = "VALOR", nullable = false, length = 150)
    private String valor;

    @Column(name = "ORDEN", nullable = false)
    private Integer orden;

    /**
     * Marca los valores que la logica del sistema referencia por codigo.
     * El administrador puede renombrar el texto visible pero no borrarlos:
     * si desaparece TITULAR, la validacion de titular unico queda sin sentido.
     */
    @Column(name = "PROTEGIDO", nullable = false, length = 1)
    private String protegido;
}
