package guardian.controller.residente;

import guardian.constant.ApiEndpoint;
import guardian.dto.residente.MiQrResponse;
import guardian.security.UsuarioActual;
import guardian.service.acceso.MiCredencialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autogestion del residente. Cualquier usuario autenticado puede consultar
 * <b>su</b> credencial; el id de la persona sale del token, nunca de la URL.
 */
@RestController
@RequestMapping(ApiEndpoint.RESIDENTE)
@RequiredArgsConstructor
public class MiCredencialController {

    private static final int TAMANO_QR_PX = 512;

    private final MiCredencialService miCredencialService;
    private final UsuarioActual usuarioActual;

    @GetMapping(ApiEndpoint.RESIDENTE_MI_QR)
    public ResponseEntity<MiQrResponse> miQr() {
        return ResponseEntity.ok(miCredencialService.miQr(usuarioActual.obtener()));
    }

    @GetMapping(value = ApiEndpoint.RESIDENTE_MI_QR_PNG, produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> miQrPng(
            @RequestParam(defaultValue = "" + TAMANO_QR_PX) int tamano) {

        // Acotado: un tamano sin tope es un OOM gratis para cualquier sesion.
        byte[] png = miCredencialService.miQrPng(
                usuarioActual.obtener(), Math.max(128, Math.min(tamano, 1024)));

        // noStore: la imagen es una credencial de acceso. Si un proxy o el disco
        // del navegador la guardan, queda una copia utilizable en un equipo
        // compartido despues de cerrar sesion.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
