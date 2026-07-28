package guardian.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * CORS centralizado. Los origenes se leen de {@code guardian.security.allowed-origins}
 * como lista separada por comas.
 *
 * <p>Nunca se usa {@code *}: el API expone datos personales de residentes —
 * nombre, casa, foto, horarios de entrada y salida — y una whitelist abierta
 * dejaria que cualquier pagina los consultara con el token del usuario.</p>
 */
@Configuration
public class CorsGlobalConfig {

    private final List<String> origenesPermitidos;

    public CorsGlobalConfig(
            @Value("${guardian.security.allowed-origins}") String origenes) {
        this.origenesPermitidos = Arrays.asList(origenes.split("\\s*,\\s*"));
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
