package guardian.service.admin;

import guardian.dto.admin.ImportacionCasasResponse;
import guardian.security.UsuarioAutenticado;
import org.springframework.web.multipart.MultipartFile;

public interface ImportacionCasasService {

    /**
     * Nombres EXACTOS de las columnas. Los usan el lector y el generador de la
     * plantilla, para que el formato que se descarga no pueda separarse del que
     * se acepta.
     */
    String COLUMNA_TIPO = "Tipo";
    String COLUMNA_NUMERO = "Numero";

    /**
     * Lee el archivo y crea las casas que pueda.
     *
     * <p>Fila a fila y sin transaccion unica a proposito: en un archivo de
     * doscientas casas, que la numero ciento ochenta este repetida no puede
     * tirar abajo las ciento setenta y nueve buenas. Lo que no entra se
     * devuelve explicado.</p>
     */
    ImportacionCasasResponse importar(MultipartFile archivo, UsuarioAutenticado ejecutor);

    /** El .xlsx de ejemplo, generado con las MISMAS columnas que se leen. */
    byte[] plantilla(Long conjuntoId);
}
