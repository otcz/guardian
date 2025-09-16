package otcz.guardian.config;


import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.utils.JwtUtil;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Collections;


@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    private UsuarioService usuarioService;

    @Autowired
    public void setUsuarioService(@Lazy UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("[JwtRequestFilter] Ejecutando filtro para: " + request.getRequestURI()); // Log de entrada
        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("[JwtRequestFilter] Header Authorization: " + authorizationHeader); // Log del header
        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("Token recibido en filtro: " + jwt); // Log del token recibido
            try {
                // Imprimir fecha de expiración y fecha actual
                java.util.Date expDate = jwtUtil.extractExpiration(jwt);
                System.out.println("Fecha de expiración del token: " + expDate);
                System.out.println("Fecha actual del sistema: " + new java.util.Date());
            } catch (Exception e) {
                System.out.println("No se pudo extraer la fecha de expiración del token: " + e.getMessage());
            }
            username = jwtUtil.extractUsername(jwt);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = usuarioService.loadUserByUsername(username);
            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                // Extraer el rol del JWT y agregarlo como autoridad
                String rol = jwtUtil.extractRol(jwt);
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + rol);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, Collections.singletonList(authority));
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);

    }
}
