package guardian.security;

import guardian.constant.Codigos;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Lee el header {@code Authorization: Bearer <token>} y, si el token verifica,
 * publica la identidad en el contexto de Spring Security.
 *
 * <p>El token prueba <b>quien es</b>; el estado — activo, rol, cambio de clave
 * pendiente — se lee fresco de {@link EstadoUsuarioService} en cada peticion.
 * Asi deshabilitar un usuario o cambiarle el rol surte efecto en segundos, no
 * cuando el JWT expire 12 horas despues.</p>
 *
 * <p>Si no hay token, no verifica o el usuario ya no puede operar, el filtro
 * deja pasar la peticion sin autenticar: quien decide si eso es un 401 es la
 * configuracion de rutas, no este filtro. Asi los endpoints publicos siguen
 * funcionando sin excepciones especiales.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIJO = "Bearer ";
    /** Spring Security espera el prefijo ROLE_ para que hasRole() funcione. */
    private static final String PREFIJO_ROL = "ROLE_";

    private final JwtService jwtService;
    private final EstadoUsuarioService estadoUsuarioService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIJO)) {
            UsuarioAutenticado delToken = jwtService.verificar(header.substring(PREFIJO.length()));

            if (delToken != null && delToken.getUsuarioId() != null) {
                Optional<EstadoUsuario> estado =
                        estadoUsuarioService.estadoDe(delToken.getUsuarioId());

                estado.filter(EstadoUsuario::isPuedeOperar)
                        .ifPresent(e -> autenticar(delToken, e));
            }
        }

        chain.doFilter(request, response);
    }

    private void autenticar(UsuarioAutenticado delToken, EstadoUsuario estado) {
        // Con la clave inicial sin cambiar, la autoridad se degrada: el unico
        // territorio permitido son los endpoints de autenticacion. Sin esto,
        // el "cambio obligatorio" seria una cortesia del frontend.
        String autoridad = estado.isCambioClavePendiente()
                ? Codigos.ROL_CLAVE_PENDIENTE
                : estado.getRol();

        UsuarioAutenticado usuario = new UsuarioAutenticado(
                delToken.getUsuarioId(),
                delToken.getPersonaId(),
                delToken.getConjuntoId(),
                delToken.getDocumento(),
                delToken.getNombreCompleto(),
                estado.getRol(),
                estado.isCambioClavePendiente());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority(PREFIJO_ROL + autoridad)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
