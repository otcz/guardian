package guardian.config;

import guardian.constant.ApiEndpoint;
import guardian.constant.Codigos;
import guardian.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de seguridad.
 *
 * <p>El API es sin estado y se consume desde una PWA con token Bearer, no con
 * cookies de sesion. Por eso CSRF va deshabilitado: sin cookie de sesion no hay
 * vector de cross-site request forgery, y dejarlo activo solo romperia las
 * peticiones sin agregar proteccion.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // Sin esto, una peticion sin token recibe un 403 que el frontend
                // no puede distinguir de "no tienes permiso". El 401 es lo que
                // dispara el redirect al login en el interceptor.
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                .authorizeRequests()

                .antMatchers(ApiEndpoint.AUTH + ApiEndpoint.AUTH_LOGIN).permitAll()
                .antMatchers("/actuator/health").permitAll()
                // Las fotos se leen sin sesion: una etiqueta <img> no puede
                // mandar Authorization. La proteccion es el nombre UUID.
                .antMatchers(ApiEndpoint.PUBLICO_FOTOS + "/**").permitAll()
                // El invitado abre su link sin cuenta; el codigo UUID es la llave.
                .antMatchers(ApiEndpoint.PUBLICO_INVITACIONES + "/**").permitAll()

                // Los endpoints de autenticacion aceptan tambien la autoridad
                // degradada CLAVE_PENDIENTE: cambiar la clave y consultar la
                // sesion es lo UNICO que puede hacer quien no la ha cambiado.
                .antMatchers(ApiEndpoint.AUTH + "/**").authenticated()

                .antMatchers(ApiEndpoint.ACCESO + "/**")
                .hasAnyRole(Codigos.ROL_GUARDIA, Codigos.ROL_ADMIN)

                .antMatchers(ApiEndpoint.ADMIN + "/**")
                .hasRole(Codigos.ROL_ADMIN)

                // El resto exige un rol REAL. authenticated() no basta: dejaria
                // pasar a CLAVE_PENDIENTE y el cambio obligatorio seria
                // decorativo a nivel de API.
                .anyRequest().hasAnyRole(
                        Codigos.ROL_ADMIN, Codigos.ROL_GUARDIA, Codigos.ROL_RESIDENTE)

                .and()
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
