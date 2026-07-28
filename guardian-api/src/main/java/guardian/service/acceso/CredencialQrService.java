package guardian.service.acceso;

import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.persona.GdPersona;

import java.util.Optional;

public interface CredencialQrService {

    /**
     * Emite la credencial permanente de una persona. Si ya tenia una activa, la
     * revoca: dos QR validos para la misma persona harian imposible saber cual
     * se comprometio.
     */
    GdCredencialQr emitirPermanente(GdPersona persona, String usuarioEjecutor);

    /** Payload que se dibuja en el QR. */
    String construirPayload(GdCredencialQr credencial);

    /** PNG del QR, para imprimir carnets de residentes sin smartphone. */
    byte[] renderizarPng(String payload, int tamanoPx);

    /**
     * Resuelve un payload escaneado.
     *
     * @return la credencial si el payload esta bien formado y la firma cuadra;
     *         {@code Optional.empty()} si no. No valida vigencia ni estado de la
     *         persona — de eso se encarga {@link AccesoService}, que ademas tiene
     *         que registrar el intento fallido.
     */
    Optional<GdCredencialQr> resolver(String payload);

    void revocar(Long credencialId, String usuarioEjecutor);
}
