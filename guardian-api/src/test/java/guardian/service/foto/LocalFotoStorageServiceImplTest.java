package guardian.service.foto;

import guardian.constant.ApiEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El borrado de fotos huerfanas.
 *
 * <p>Cada subida crea un archivo nuevo con nombre UUID, asi que reemplazar una
 * foto dejaba la anterior tirada en disco — y se sigue sirviendo publicamente
 * por su nombre. Estos casos cubren la parte que puede hacer dano: distinguir
 * "la cambio" de "mando la misma".</p>
 */
class LocalFotoStorageServiceImplTest {

    @TempDir
    Path directorio;

    private LocalFotoStorageServiceImpl storage;

    @BeforeEach
    void preparar() {
        storage = new LocalFotoStorageServiceImpl(directorio.toString());
    }

    @Test
    @DisplayName("al reemplazar por otra, la anterior se borra del disco")
    void borraLaAnteriorCuandoCambia() throws IOException {
        String anterior = storage.guardar(new byte[]{1, 2, 3}, "jpg");
        String nueva = storage.guardar(new byte[]{4, 5, 6}, "jpg");

        storage.eliminarReemplazada(url(anterior), url(nueva));

        assertThat(Files.exists(directorio.resolve(anterior))).isFalse();
        assertThat(Files.exists(directorio.resolve(nueva))).isTrue();
    }

    @Test
    @DisplayName("mandar LA MISMA foto no borra nada")
    void noBorraCuandoEsLaMisma() {
        // El caso que no puede fallar. El formulario de edicion reenvia la foto
        // actual cada vez que se corrige el color o la placa: si esto borrara,
        // guardar un cambio de color dejaria a la fila apuntando a un archivo
        // inexistente, y eso es lo que la porteria muestra.
        String foto = storage.guardar(new byte[]{1, 2, 3}, "jpg");

        storage.eliminarReemplazada(url(foto), url(foto));

        assertThat(Files.exists(directorio.resolve(foto))).isTrue();
    }

    @Test
    @DisplayName("quitar la foto (queda sin ninguna) borra la anterior")
    void borraCuandoLaNuevaEsNula() {
        String foto = storage.guardar(new byte[]{1, 2, 3}, "jpg");

        storage.eliminarReemplazada(url(foto), null);

        assertThat(Files.exists(directorio.resolve(foto))).isFalse();
    }

    @Test
    @DisplayName("una ruta que no es una subida propia no toca el disco")
    void ignoraRutasAjenas() {
        // El nombre nunca llega a resolverse contra el sistema de archivos:
        // es la misma defensa de path traversal que ya tenia la lectura.
        String foto = storage.guardar(new byte[]{1, 2, 3}, "jpg");

        storage.eliminarReemplazada("https://otro-sitio.com/carro.jpg", url(foto));
        storage.eliminarPorUrl("../../application.properties");
        storage.eliminarPorUrl(ApiEndpoint.PUBLICO_FOTOS + "/../secreto.txt");

        assertThat(Files.exists(directorio.resolve(foto))).isTrue();
    }

    private String url(String nombreArchivo) {
        return ApiEndpoint.PUBLICO_FOTOS + "/" + nombreArchivo;
    }
}
