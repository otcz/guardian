package guardian.service.foto;

public interface FotoStorageService {

    /**
     * Persiste la imagen y devuelve el nombre de archivo asignado.
     * El nombre es un UUID: no revela nada de la persona y no es adivinable,
     * que es lo que permite servir las fotos sin sesion (la etiqueta img del
     * navegador no puede mandar el header Authorization).
     */
    String guardar(byte[] contenido, String extension);

    /** @return el contenido, o null si no existe. */
    byte[] leer(String nombreArchivo);
}
