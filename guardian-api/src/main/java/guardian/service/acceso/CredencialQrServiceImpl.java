package guardian.service.acceso;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.persona.GdPersona;
import guardian.exception.GuardianException;
import guardian.repository.GdCredencialQrRepository;
import guardian.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Emision y verificacion de las credenciales QR.
 *
 * <p><b>Formato del payload:</b> {@code GRD1.<codigoPublico>.<firma>}</p>
 *
 * <p>No lleva ningun dato personal — ni nombre, ni casa, ni documento — solo un
 * UUID opaco y su firma. Quien fotografie el QR de un vecino no obtiene
 * informacion, y quien intercepte uno no puede fabricar otro sin la clave
 * HMAC.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredencialQrServiceImpl implements CredencialQrService {

    /** Prefijo con version. Si algun dia cambia el esquema, los lectores viejos lo distinguen. */
    private static final String PREFIJO = "GRD1";
    private static final String SEPARADOR = ".";
    private static final int PARTES_ESPERADAS = 3;

    private final GdCredencialQrRepository credencialRepository;

    @Value("${guardian.security.qr-hmac-secret}")
    private String secretoHmac;

    @Override
    @Transactional
    public GdCredencialQr emitirPermanente(GdPersona persona, String usuarioEjecutor) {
        // La foto es el control real del sistema: sin ella el guardia no puede
        // confirmar que quien muestra el QR es su dueno, y la credencial se
        // vuelve un pase transferible. Ver .claude/CONTEXT.md seccion 3.
        if (persona.getFotoUrl() == null || persona.getFotoUrl().trim().isEmpty()) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.PERSONA_SIN_FOTO);
        }

        credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .ifPresent(anterior -> {
                    anterior.setActivo(Codigos.NO);
                    anterior.setUsuarioModificador(usuarioEjecutor);
                    anterior.setObservaciones("Revocada al emitir una credencial nueva");
                    credencialRepository.save(anterior);
                    log.info("[credencial] revocada por reemision id={} personaId={}",
                            anterior.getId(), persona.getId());
                });

        String codigoPublico = UUID.randomUUID().toString();

        GdCredencialQr credencial = new GdCredencialQr();
        credencial.setPersona(persona);
        credencial.setCodigoPublico(codigoPublico);
        credencial.setFirmaHash(HmacUtil.firmar(codigoPublico, secretoHmac));
        credencial.setTipo(Codigos.CREDENCIAL_PERMANENTE);
        credencial.setUsosRealizados(0);
        credencial.setActivo(Codigos.SI);
        credencial.setUsuarioCreador(usuarioEjecutor);

        GdCredencialQr guardada = credencialRepository.save(credencial);
        log.info("[credencial] emitida id={} personaId={}", guardada.getId(), persona.getId());
        return guardada;
    }

    @Override
    public String construirPayload(GdCredencialQr credencial) {
        return PREFIJO + SEPARADOR + credencial.getCodigoPublico()
                + SEPARADOR + credencial.getFirmaHash();
    }

    @Override
    public byte[] renderizarPng(String payload, int tamanoPx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            // Correccion alta: el carnet impreso se raya, se moja y se dobla en
            // el bolsillo. Con nivel H el QR sigue leyendo con ~30% del area
            // danada, que es la diferencia entre reimprimir carnets cada mes o no.
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matriz = new QRCodeWriter()
                    .encode(payload, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx, hints);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();

        } catch (Exception ex) {
            log.error("[credencial] fallo el render del QR", ex);
            throw new IllegalStateException("No se pudo generar la imagen del QR", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GdCredencialQr> resolver(String payload) {
        if (payload == null) {
            return Optional.empty();
        }

        String[] partes = payload.trim().split("\\" + SEPARADOR);
        if (partes.length != PARTES_ESPERADAS || !PREFIJO.equals(partes[0])) {
            return Optional.empty();
        }

        String codigoPublico = partes[1];
        String firmaRecibida = partes[2];

        // Se recalcula la firma ANTES de ir a la base. Un codigo con firma
        // invalida ni siquiera merece un query: asi un ataque de fuerza bruta
        // contra la porteria no puede saturar la base de datos.
        String firmaEsperada = HmacUtil.firmar(codigoPublico, secretoHmac);
        if (!HmacUtil.coincide(firmaEsperada, firmaRecibida)) {
            log.info("[credencial] firma invalida codigo={}", codigoPublico);
            return Optional.empty();
        }

        return credencialRepository.buscarPorCodigoPublico(codigoPublico);
    }

    @Override
    @Transactional
    public void revocar(Long credencialId, String usuarioEjecutor) {
        GdCredencialQr credencial = credencialRepository.findById(credencialId)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        credencial.setActivo(Codigos.NO);
        credencial.setUsuarioModificador(usuarioEjecutor);
        credencialRepository.save(credencial);

        log.info("[credencial] revocada id={} por={}", credencialId, usuarioEjecutor);
    }
}
