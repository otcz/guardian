package guardian.security;

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

/**
 * Lee el header {@code Authorization: Bearer <token>} y, si el token verifica,
 * publica la identidad en el contexto de Spring Security.
 *
 * <p>Si no hay token o no verifica, el filtro deja pasar la peticion sin
 * autenticar: quien decide si eso es un 401 es la configuracion de rutas, no
 * este filtro. Asi los endpoints publicos siguen funcionando sin excepciones
 * especiales.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIJO = "Bearer ";
    /** Spring Security espera el prefijo ROLE_ para que hasRole() funcione. */
    private static final String PREFIJO_ROL = "ROLE_";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIJO)) {
            UsuarioAutenticado usuario = jwtService.verificar(header.substring(PREFIJO.length()));

            if (usuario != null && usuario.getRol() != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                Collections.singletonList(
                                        new SimpleGrantedAuthority(PREFIJO_ROL + usuario.getRol())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
