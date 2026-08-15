package guardian.entity.persona;

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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * La huella registrada de una persona.
 *
 * <p><b>Dato sensible.</b> Bajo la Ley 1581 de 2012 la huella no es un dato
 * personal cualquiera: exige autorizacion previa, expresa y escrita de su
 * titular, con la finalidad declarada, y NO puede ser el unico medio de acceso
 * — quien no autorice tiene que poder entrar por codigo o por documento. Por
 * eso la huella en GUARDIAN es un tercer camino y nunca un reemplazo de los
 * otros dos.</p>
 *
 * <p><b>Nunca la imagen del dedo, solo la plantilla.</b> La plantilla es el
 * conjunto de rasgos que extrae el algoritmo; de ella no se puede reconstruir
 * la huella. Guardar la imagen seria guardar el dato biometrico en crudo, que
 * es exactamente lo que no se debe hacer.</p>
 *
 * <p><b>Por que se guarda el algoritmo.</b> Una plantilla solo la entiende el
 * algoritmo que la genero. El dia que se cambie de lector o de SDK, esta
 * columna es lo que permite saber cuales hay que volver a tomar en vez de
 * descubrirlo cuando a alguien no le abra la puerta.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "GD_HUELLA",
        uniqueConstraints = @UniqueConstraint(
                // Un dedo por persona, una sola vez. Sin esto, reintentar un
                // enrolamiento fallido acumula plantillas del mismo dedo y el
                // cotejo se vuelve mas lento sin ser mas certero.
                name = "UK_HUELLA_PERSONA_DEDO",
                columnNames = {"PERSONA_ID", "DEDO"}
        )
)
public class GdHuella extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERSONA_ID", nullable = false)
    private GdPersona persona;

    /** DERECHO o IZQUIERDO. Dos por persona: ver HuellaService. */
    @Column(name = "DEDO", nullable = false, length = 20)
    private String dedo;

    /**
     * La plantilla, tal como la entrega el algoritmo.
     *
     * <p>Bytes y no texto: es binaria. Se guarda como esta y NO se indexa —
     * el cotejo biometrico no es una comparacion de igualdad, es un calculo de
     * similitud que hace el algoritmo dedo por dedo.</p>
     */
    @Lob
    @Column(name = "PLANTILLA", nullable = false)
    private byte[] plantilla;

    /** Quien la genero: "ZKFinger V10.0", "SourceAFIS 3.x". Ver el javadoc. */
    @Column(name = "ALGORITMO", nullable = false, length = 40)
    private String algoritmo;

    /**
     * Calidad que reporto el lector al capturarla, de 0 a 100.
     *
     * <p>Se guarda para poder responder por que a alguien no le abre: una
     * plantilla de calidad 30 falla a menudo y hay que volver a tomarla. Sin
     * este dato, ese caso se investiga a ciegas.</p>
     */
    @Column(name = "CALIDAD")
    private Integer calidad;
}
