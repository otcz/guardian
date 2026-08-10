package guardian.entity.persona;

import guardian.entity.base.BaseEntity;
import guardian.entity.conjunto.GdConjunto;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import java.util.Date;

/**
 * Persona fisica: residentes, guardias y administradores.
 *
 * <p>Se modela una sola tabla para todos porque un guardia puede vivir en el
 * conjunto y un administrador puede ser residente. Separarlos obligaria a
 * duplicar a esa persona y a mantener dos fotos y dos documentos sincronizados.
 * Lo que distingue a cada uno es el rol de su {@link GdUsuario}, no su
 * naturaleza.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_PERSONA",
        uniqueConstraints = {
                // Unicidad GLOBAL, no por sede: una cedula identifica a UNA
                // persona en todo el sistema. Asi la misma persona no puede
                // existir dos veces aunque las sedes sean distintas.
                //
                // El telefono NO va aca a proposito: es un dato de contacto,
                // no una llave (nada en el sistema busca por telefono), y en
                // la vida real una casa comparte un solo numero entre varios
                // miembros. Exigirlo unico le impedia a una pareja registrarse
                // con el mismo celular.
                @UniqueConstraint(name = "UK_PERSONA_DOCUMENTO", columnNames = "DOCUMENTO")
        }
)
public class GdPersona extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CONJUNTO_ID", nullable = false)
    private GdConjunto conjunto;

    /**
     * Codigo del grupo TIPO_DOCUMENTO: CC, TI, CE, PA, RC. El default en
     * columna cubre las filas creadas antes de esta migracion.
     */
    @Column(name = "TIPO_DOCUMENTO", nullable = false, length = 5,
            columnDefinition = "varchar(5) default 'CC'")
    private String tipoDocumento;

    @Column(name = "DOCUMENTO", nullable = false, length = 20)
    private String documento;

    @Column(name = "NOMBRES", nullable = false, length = 100)
    private String nombres;

    @Column(name = "APELLIDOS", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "FECHA_NACIMIENTO")
    @Temporal(TemporalType.DATE)
    private Date fechaNacimiento;

    /**
     * Ruta o URL de la foto.
     *
     * <p>Es el control de seguridad central del sistema, no un adorno: el QR
     * dice quien dice ser la persona, y la foto es lo que deja al guardia
     * confirmar que quien esta parado al frente es esa. Por eso no se emite
     * credencial a una persona sin foto.</p>
     */
    @Column(name = "FOTO_URL", length = 500)
    private String fotoUrl;

    @Column(name = "TELEFONO", length = 30)
    private String telefono;

    @Column(name = "EMAIL", length = 120)
    private String email;

    // OJO: no agregar aca un getConjuntoId(). Spring Data lo tomaria como un
    // atributo de la entidad y romperia countByConjuntoId y las demas consultas
    // derivadas, que navegan por la relacion 'conjunto'.

    public String getNombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }
}
