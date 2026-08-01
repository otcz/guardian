package guardian.service.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import guardian.constant.MensajesGlobales;
import guardian.exception.GuardianException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class IntentosLoginServiceImpl implements IntentosLoginService {

    private static final long MAXIMO_DOCUMENTOS_EN_CACHE = 10_000;

    private final int maxIntentos;
    private final Cache<String, AtomicInteger> fallos;

    public IntentosLoginServiceImpl(
            @Value("${guardian.security.login-max-intentos}") int maxIntentos,
            @Value("${guardian.security.login-bloqueo-minutos}") long bloqueoMinutos) {
        this.maxIntentos = maxIntentos;
        // La ventana del contador ES la ventana de bloqueo: al expirar la
        // entrada, el contador vuelve a cero y el documento queda libre.
        this.fallos = Caffeine.newBuilder()
                .expireAfterWrite(bloqueoMinutos, TimeUnit.MINUTES)
                .maximumSize(MAXIMO_DOCUMENTOS_EN_CACHE)
                .build();
    }

    @Override
    public void exigirNoBloqueado(String documento) {
        AtomicInteger contador = fallos.getIfPresent(llave(documento));
        if (contador != null && contador.get() >= maxIntentos) {
            // warn y no info: un documento bloqueado es o un ataque o un vecino
            // desesperado, y ambos le interesan al administrador.
            log.warn("[auth] login bloqueado por intentos documento={}", documento);
            throw GuardianException.noAutorizado(MensajesGlobales.CREDENCIALES_INVALIDAS);
        }
    }

    @Override
    public void registrarFallo(String documento) {
        int total = fallos.get(llave(documento), d -> new AtomicInteger()).incrementAndGet();
        if (total == maxIntentos) {
            log.warn("[auth] documento={} alcanzo {} fallos y queda bloqueado temporalmente",
                    documento, total);
        }
    }

    @Override
    public void limpiar(String documento) {
        fallos.invalidate(llave(documento));
    }

    /** El login ignora mayusculas; el contador debe ignorar lo mismo. */
    private String llave(String documento) {
        return documento == null ? "" : documento.trim().toUpperCase();
    }
}
