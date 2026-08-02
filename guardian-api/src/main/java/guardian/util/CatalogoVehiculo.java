package guardian.util;

import guardian.constant.Codigos;
import guardian.service.admin.ParametroService;

/**
 * Las reglas de catalogo de un vehiculo, en un solo sitio.
 *
 * <p>El vehiculo se da de alta desde dos lados —el panel del administrador y
 * "Mi hogar" del residente— y las dos altas tienen que validar igual. Duplicada,
 * la validacion se corrige en un lado y se olvida en el otro.</p>
 */
public final class CatalogoVehiculo {

    private CatalogoVehiculo() {
    }

    /**
     * Marca y color son opcionales: no todo el mundo sabe de que marca es el
     * carro que maneja, y obligar a llenarlos solo produce datos inventados.
     * Cuando vienen, tienen que ser del catalogo.
     */
    public static void exigir(ParametroService parametros, String tipo, String marca, String color) {
        parametros.exigirCodigoValido(Codigos.GRUPO_TIPO_VEHICULO, tipo);

        String marcaLimpia = limpiar(marca);
        if (marcaLimpia != null) {
            parametros.exigirCodigoValido(Codigos.GRUPO_MARCA_VEHICULO, marcaLimpia);
        }
        String colorLimpio = limpiar(color);
        if (colorLimpio != null) {
            parametros.exigirCodigoValido(Codigos.GRUPO_COLOR_VEHICULO, colorLimpio);
        }
    }

    /**
     * Cadena vacia y null significan lo mismo —no lo declaro— y guardar las dos
     * dejaria dos formas de estar vacio que hay que preguntar por separado.
     */
    public static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
