package guardian.service.admin;

/**
 * Traduce un codigo del catalogo al texto que se le muestra a una persona.
 *
 * <p>Existe porque el codigo es lo que se guarda —VOLKSWAGEN, VINOTINTO— y lo
 * que se lee tiene que ser "Volkswagen" y "Vinotinto". Sin esto, la ficha de la
 * garita mostraria el codigo en mayusculas sostenidas y el guardia estaria
 * comparando texto tecnico contra un carro real.</p>
 *
 * <p>Va aparte de {@link ParametroService} a proposito: aquel administra el
 * catalogo, este solo lo lee. Y lee mucho — una vez por vehiculo en cada ficha —
 * asi que tiene su propio cache.</p>
 */
public interface EtiquetaCatalogoService {

    /**
     * @return el texto visible, o el codigo tal cual si no esta en el catalogo.
     *         Devolver el codigo y no null es deliberado: los vehiculos
     *         registrados antes de que marca y color fueran catalogo tienen
     *         texto libre guardado, y es mejor mostrarlo que dejar el campo en
     *         blanco en la ficha de la porteria.
     */
    String etiqueta(String grupo, String codigo);

    /** Lo llama el administrador al tocar el catalogo. */
    void invalidar(String grupo);
}
