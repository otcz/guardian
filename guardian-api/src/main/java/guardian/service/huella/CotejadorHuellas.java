package guardian.service.huella;

import guardian.entity.persona.GdHuella;

import java.util.List;
import java.util.Optional;

/**
 * LA COSTURA. Lo unico de las huellas que depende del lector que se compre.
 *
 * <p>Todo lo demas —la tabla, el enrolamiento, los permisos, la pantalla, el
 * registro del paso— esta construido y no cambia. Cuando se decida el hardware
 * se escribe UNA implementacion de esta interfaz y el sistema queda operando.
 * Esa separacion es deliberada: sin ella, elegir mal el lector obligaria a
 * rehacer medio modulo.</p>
 *
 * <p><b>Las dos opciones que van a competir por implementarla:</b></p>
 *
 * <ul>
 *   <li><b>SourceAFIS</b> (Java, corre en Linux). Sirve si el SDK del lector
 *   entrega la IMAGEN de la huella. El cotejo ocurre en nuestro servidor y las
 *   plantillas nunca salen de el — lo correcto para un dato sensible.</li>
 *
 *   <li><b>El SDK del fabricante</b> (Windows). Si el lector solo entrega su
 *   plantilla propietaria, el cotejo tiene que correr donde ese SDK pueda
 *   instalarse, y habria que repartirle las plantillas a cada porteria. Peor,
 *   pero es el camino si no hay imagen.</li>
 * </ul>
 *
 * <p>Cual de las dos se usa depende de UNA pregunta al SDK: si entrega imagen
 * o solo plantilla.</p>
 */
public interface CotejadorHuellas {

    /**
     * Si el cotejo esta operativo.
     *
     * <p>La pantalla pregunta esto para decir "sensor no conectado" en vez de
     * ofrecer un boton que no hace nada.</p>
     */
    boolean estaDisponible();

    /**
     * Con que algoritmo se generan y comparan las plantillas.
     *
     * <p>Se guarda junto a cada huella: el dia que se cambie de lector, es lo
     * que permite saber cuales hay que volver a tomar.</p>
     */
    String algoritmo();

    /**
     * Funde las lecturas de un mismo dedo en UNA plantilla.
     *
     * <p>Las tres capturas no son tres huellas: son tres vistas del mismo dedo
     * que el algoritmo combina para que la plantilla resultante aguante que
     * venga torcido, seco o sucio.</p>
     *
     * @return la plantilla, o vacio si las lecturas no dan para una utilizable
     *         — que es un resultado legitimo y hay que volver a capturar.
     */
    Optional<byte[]> fundir(List<byte[]> lecturas);

    /**
     * De quien es este dedo. El cotejo 1:N.
     *
     * @param leida     lo que acaba de capturar el lector
     * @param candidatas las huellas contra las que comparar
     * @return la huella que coincide, o vacio si ninguna. Vacio NO es un error:
     *         es la respuesta correcta cuando quien puso el dedo no esta
     *         registrado, y el modulo entonces ofrece enrolarlo.
     */
    Optional<GdHuella> identificar(byte[] leida, List<GdHuella> candidatas);
}
