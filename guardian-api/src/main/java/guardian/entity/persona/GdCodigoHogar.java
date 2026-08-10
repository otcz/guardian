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
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import java.util.Date;

/**
 * Codigo con el que alguien se registra dentro de un hogar YA existente.
 *
 * <p>Lo genera el titular y se lo pasa a su familiar por donde quiera. Con el,
 * esa persona crea su propia cuenta sin que el administrador tenga que
 * digitarla: la via que faltaba para que un nucleo se arme solo.</p>
 *
 * <p><b>Un solo uso y con vencimiento, a proposito.</b> Este codigo no abre
 * una puerta: crea una persona DENTRO del conjunto, y esa persona termina con
 * credencial para entrar. Si se filtra, un desconocido queda adentro. Un uso
 * unico deja la ventana en una sola persona, el vencimiento la cierra sola, y
 * el titular ve en su hogar exactamente quien lo uso.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_CODIGO_HOGAR",
        indexes = {
                @Index(name = "IX_CODIGO_HOGAR_CODIGO", columnList = "CODIGO", unique = true),
                @Index(name = "IX_CODIGO_HOGAR_CASA", columnList = "CASA_ID")
        }
)
public class GdCodigoHogar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CONJUNTO_ID", nullable = false)
    private Long conjuntoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CASA_ID", nullable = false)
    private GdCasa casa;

    /**
     * El titular que lo genero. A el se le rinde cuentas de quien entro.
     *
     * <p>Nullable a proposito, aunque nace siempre con uno: si el titular se
     * elimina despues, el codigo sigue existiendo como historial de quien
     * uso ese enlace, solo que ya no sabe decir quien lo genero.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TITULAR_ID")
    private GdPersona titular;

    /** UUID no adivinable: es la unica llave, igual que en las invitaciones. */
    @Column(name = "CODIGO", nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(name = "VIGENCIA_HASTA", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date vigenciaHasta;

    /**
     * Quien lo uso. Null mientras no se haya usado — y es lo que el titular ve
     * en su pantalla para saber si su codigo sigue esperando o ya sirvio.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSONA_REGISTRADA_ID")
    private GdPersona personaRegistrada;

    /**
     * Dos personas abriendo el mismo enlace a la vez pasarian las dos la
     * validacion antes de que ninguna guarde. Con @Version la segunda escritura
     * falla y solo una queda registrada.
     */
    @Version
    @Column(name = "VERSION")
    private Long version;

    public boolean estaUsado() {
        return personaRegistrada != null;
    }

    public boolean estaVigente(Date momento) {
        return vigenciaHasta != null && momento.before(vigenciaHasta);
    }

    /** Sirve solo si esta activo, sin usar y dentro de su vigencia. */
    public boolean sirve(Date momento) {
        return puedeOperar() && !estaUsado() && estaVigente(momento);
    }
}
