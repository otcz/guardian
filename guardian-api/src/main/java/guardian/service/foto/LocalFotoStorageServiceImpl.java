package guardian.service.foto;

import guardian.util.FotoUrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Almacenamiento de fotos en disco local. Suficiente para un conjunto
 * residencial (unas centenas de imagenes); si el producto crece a GCS, esta
 * interface es el unico punto de cambio.
 */
@Slf4j
@Service
public class LocalFotoStorageServiceImpl implements FotoStorageService {

    /**
     * Solo se leen nombres con la forma exacta UUID.extension. Es la defensa
     * contra path traversal: "../" o cualquier otra cosa ni siquiera se
     * intenta resolver contra el disco.
     */
    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{2,5}$");

    private final Path directorioBase;

    public LocalFotoStorageServiceImpl(@Value("${storage.local.path}") String ruta) {
        this.directorioBase = Paths.get(ruta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directorioBase);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de fotos: " + directorioBase, ex);
        }
    }

    @Override
    public String guardar(byte[] contenido, String extension) {
        String nombre = UUID.randomUUID() + "." + extension.toLowerCase();
        try {
            Files.write(directorioBase.resolve(nombre), contenido);
            log.info("[foto] guardada nombre={} bytes={}", nombre, contenido.length);
            return nombre;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la foto", ex);
        }
    }

    @Override
    public byte[] leer(String nombreArchivo) {
        if (nombreArchivo == null || !NOMBRE_VALIDO.matcher(nombreArchivo).matches()) {
            return null;
        }
        try {
            Path archivo = directorioBase.resolve(nombreArchivo);
            return Files.exists(archivo) ? Files.readAllBytes(archivo) : null;
        } catch (IOException ex) {
            log.warn("[foto] fallo la lectura nombre={}", nombreArchivo, ex);
            return null;
        }
    }

    @Override
    public void eliminarPorUrl(String fotoUrl) {
        String nombre = FotoUrlUtil.nombreArchivoDe(fotoUrl);
        if (nombre != null) {
            alConfirmar(nombre);
        }
    }

    @Override
    public void eliminarReemplazada(String urlAnterior, String urlNueva) {
        String anterior = FotoUrlUtil.nombreArchivoDe(urlAnterior);
        if (anterior == null || anterior.equals(FotoUrlUtil.nombreArchivoDe(urlNueva))) {
            return;
        }
        alConfirmar(anterior);
    }

    /**
     * Borra el archivo DESPUES de que la transaccion confirme.
     *
     * <p>Borrar en el momento es una perdida de datos esperando a ocurrir: si
     * lo que sigue en el metodo revienta —vincular la casa, la unicidad de la
     * placa— la transaccion revierte, la entidad se queda con su foto
     * ANTERIOR, y el archivo de esa foto ya no existe. La fila queda apuntando
     * a una imagen rota y no hay forma de recuperarla.</p>
     *
     * <p>Sin transaccion activa borra directo: asi el metodo sirve igual desde
     * un contexto que no sea transaccional.</p>
     */
    private void alConfirmar(String nombreArchivo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eliminar(nombreArchivo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eliminar(nombreArchivo);
                    }
                });
    }

    @Override
    public void eliminar(String nombreArchivo) {
        // La misma defensa de path traversal que la lectura: un nombre que no
        // sea UUID.extension ni siquiera se resuelve contra el disco.
        if (nombreArchivo == null || !NOMBRE_VALIDO.matcher(nombreArchivo).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(directorioBase.resolve(nombreArchivo));
            log.info("[foto] eliminada nombre={}", nombreArchivo);
        } catch (IOException ex) {
            // No se propaga: la eliminacion de la persona ya ocurrio y revertir
            // la transaccion por un archivo dejaria el estado peor, no mejor.
            log.warn("[foto] no se pudo eliminar nombre={}", nombreArchivo, ex);
        }
    }
}
