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

    /**
     * Borra el archivo si existe. Se usa al eliminar fisicamente a la persona:
     * la foto es publica por nombre y no debe sobrevivir a su dueno.
     */
    void eliminar(String nombreArchivo);

    /**
     * Lo mismo, pero recibiendo la URL que guarda la entidad en vez del nombre
     * del archivo. Ignora null, vacio y cualquier ruta que no sea una subida
     * propia.
     *
     * <p>Existe para que los services no repitan el recorte de la ruta: estaba
     * copiado en PersonaServiceImpl y en VehiculoServiceImpl, y el dia que se
     * afloje en uno queda un hueco silencioso en el otro.</p>
     */
    void eliminarPorUrl(String fotoUrl);

    /**
     * Borra la foto ANTERIOR cuando se reemplaza por otra.
     *
     * <p>No hace nada si las dos son la misma. Eso no es un caso raro sino el
     * normal: el formulario de edicion reenvia la foto actual cada vez que se
     * corrige el color o la placa, y borrarla ahi dejaria a la entidad
     * apuntando a un archivo que ya no existe.</p>
     */
    void eliminarReemplazada(String urlAnterior, String urlNueva);
}
