package guardian.service.admin;

import guardian.dto.admin.ImportacionPersonasResponse;
import guardian.security.UsuarioAutenticado;
import org.springframework.web.multipart.MultipartFile;

public interface ImportacionPersonasService {

    /**
     * Nombres EXACTOS de las columnas. Los usan el lector y el generador de la
     * plantilla, para que el formato que se descarga no pueda separarse del que
     * se acepta.
     *
     * <p>Apellidos va aparte de Nombres porque la persona los guarda separados,
     * y partir un "nombre completo" por el ultimo espacio se equivoca con
     * cualquier apellido compuesto — "Juan de la Cruz" quedaria como Juan de la
     * / Cruz.</p>
     */
    String COLUMNA_DOCUMENTO = "Identificacion";
    String COLUMNA_NOMBRES = "Nombres";
    String COLUMNA_APELLIDOS = "Apellidos";
    String COLUMNA_NACIMIENTO = "Fecha de nacimiento";
    String COLUMNA_CORREO = "Correo";
    String COLUMNA_TELEFONO = "Telefono";

    /**
     * Lee el archivo y crea las personas que pueda, cada una con su cuenta de
     * residente y el PIN inicial.
     *
     * <p>Fila a fila y sin transaccion unica: que la persona ciento ochenta
     * este repetida no puede tirar abajo las ciento setenta y nueve buenas.</p>
     */
    ImportacionPersonasResponse importar(MultipartFile archivo, UsuarioAutenticado ejecutor);

    byte[] plantilla();
}
