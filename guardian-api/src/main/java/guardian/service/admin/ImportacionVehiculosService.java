package guardian.service.admin;

import guardian.dto.admin.ImportacionVehiculosResponse;
import guardian.security.UsuarioAutenticado;
import org.springframework.web.multipart.MultipartFile;

public interface ImportacionVehiculosService {

    /**
     * Nombres EXACTOS de las columnas. Los usan el lector y el generador de la
     * plantilla, para que el formato que se descarga no pueda separarse del que
     * se acepta.
     *
     * <p>La casa se identifica por su NOMBRE —"CASA-101"— y no por un id
     * interno: el administrador tiene delante la lista de casas, no la tabla.
     * </p>
     */
    String COLUMNA_CASA = "Casa";
    String COLUMNA_PLACA = "Placa";
    String COLUMNA_TIPO = "Tipo";
    String COLUMNA_MARCA = "Marca";
    String COLUMNA_COLOR = "Color";

    /**
     * Lee el archivo y crea los vehiculos que pueda.
     *
     * <p>Fila a fila y sin transaccion unica: que la placa ciento ochenta este
     * repetida no puede tirar abajo las ciento setenta y nueve buenas.</p>
     */
    ImportacionVehiculosResponse importar(MultipartFile archivo, UsuarioAutenticado ejecutor);

    /**
     * El .xlsx de ejemplo. Lleva una segunda hoja con los valores validos de
     * tipo, marca y color de ESTA sede: sin ella, el administrador tiene que
     * adivinarlos o volver a la pantalla a mirarlos uno por uno.
     */
    byte[] plantilla(Long conjuntoId);
}
