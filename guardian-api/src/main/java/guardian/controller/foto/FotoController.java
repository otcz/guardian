package guardian.controller.foto;

import guardian.constant.ApiEndpoint;
import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;
import guardian.service.foto.FotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Carga y entrega de fotos de personas.
 *
 * <p>La subida exige sesion. La lectura es publica porque una etiqueta img no
 * puede mandar el header Authorization; la proteccion es el nombre UUID no
 * adivinable (ver {@link FotoStorageService}).</p>
 */
@RestController
@RequiredArgsConstructor
public class FotoController {

    private static final List<String> EXTENSIONES = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final FotoStorageService fotoStorageService;

    @PostMapping(ApiEndpoint.FOTOS)
    public ResponseEntity<Map<String, String>> subir(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {

        if (archivo.isEmpty() || archivo.getSize() > MAX_BYTES) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.FOTO_INVALIDA);
        }

        String extension = extensionDe(archivo.getOriginalFilename());
        if (!EXTENSIONES.contains(extension)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.FOTO_INVALIDA);
        }

        String nombre = fotoStorageService.guardar(archivo.getBytes(), extension);
        return ResponseEntity.ok(Collections.singletonMap(
                "url", ApiEndpoint.PUBLICO_FOTOS + "/" + nombre));
    }

    @GetMapping(ApiEndpoint.PUBLICO_FOTOS + "/{nombre:.+}")
    public ResponseEntity<byte[]> ver(@PathVariable String nombre) {
        byte[] contenido = fotoStorageService.leer(nombre);
        if (contenido == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .contentType(tipoDe(nombre))
                .body(contenido);
    }

    private String extensionDe(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase();
    }

    private MediaType tipoDe(String nombre) {
        if (nombre.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (nombre.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
