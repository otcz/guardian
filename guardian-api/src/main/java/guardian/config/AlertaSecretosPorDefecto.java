package guardian.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Grita en el arranque si algun secreto sigue siendo el default del repo.
 *
 * <p>Los defaults de application.properties existen para que el dev local
 * arranque sin configurar nada, pero estan publicados en git: en cualquier
 * ambiente real son equivalentes a no tener secreto. No se tumba el arranque —
 * en dev es lo normal — pero el WARN queda en el log y en Cloud Logging, donde
 * un despliegue descuidado se nota.</p>
 */
@Slf4j
@Component
public class AlertaSecretosPorDefecto implements ApplicationRunner {

    private static final String JWT_DEFAULT = "guardian-local-dev-secret-change-me-32b!";
    private static final String QR_DEFAULT = "guardian-local-qr-hmac-change-me-32bytes!";
    private static final String ADMIN_CLAVE_DEFAULT = "230614";

    private final String jwtSecret;
    private final String qrSecret;
    private final String adminClave;

    public AlertaSecretosPorDefecto(
            @Value("${guardian.security.jwt-secret}") String jwtSecret,
            @Value("${guardian.security.qr-hmac-secret}") String qrSecret,
            @Value("${guardian.bootstrap.admin-clave}") String adminClave) {
        this.jwtSecret = jwtSecret;
        this.qrSecret = qrSecret;
        this.adminClave = adminClave;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (JWT_DEFAULT.equals(jwtSecret)) {
            log.warn("[config] GUARDIAN_JWT_SECRET es el default del repo — "
                    + "solo aceptable en desarrollo local");
        }
        if (QR_DEFAULT.equals(qrSecret)) {
            log.warn("[config] GUARDIAN_QR_HMAC_SECRET es el default del repo — "
                    + "cualquiera podria fabricar credenciales QR validas");
        }
        if (ADMIN_CLAVE_DEFAULT.equals(adminClave)) {
            log.warn("[config] la clave del administrador inicial es la default del repo — "
                    + "cambiala con GUARDIAN_ADMIN_CLAVE");
        }
    }
}
