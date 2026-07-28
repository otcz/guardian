package guardian.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Emision y verificacion del JWT de sesion (HS256).
 *
 * <p>La verificacion valida <b>firma y expiracion</b>, no solo decodifica. Un
 * token que solo se decodifica en Base64 no prueba nada: cualquiera puede armar
 * uno con {@code rol: ADMIN} y quedarse con el conjunto entero.</p>
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_PERSONA = "personaId";
    private static final String CLAIM_CONJUNTO = "conjuntoId";
    private static final String CLAIM_DOCUMENTO = "documento";
    private static final String CLAIM_NOMBRE = "nombre";
    private static final String CLAIM_ROL = "rol";

    private static final long MILIS_POR_HORA = 3_600_000L;

    private final SecretKey clave;
    private final long ttlMilis;

    public JwtService(@Value("${guardian.security.jwt-secret}") String secreto,
                      @Value("${guardian.security.jwt-ttl-horas}") long ttlHoras) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // HS256 exige 256 bits. Fallar al arrancar es preferible a arrancar
            // con una clave debil que nadie va a notar hasta que sea tarde.
            throw new IllegalStateException(
                    "guardian.security.jwt-secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.ttlMilis = ttlHoras * MILIS_POR_HORA;
    }

    public String emitir(UsuarioAutenticado usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(usuario.getUsuarioId()))
                .claim(CLAIM_PERSONA, usuario.getPersonaId())
                .claim(CLAIM_CONJUNTO, usuario.getConjuntoId())
                .claim(CLAIM_DOCUMENTO, usuario.getDocumento())
                .claim(CLAIM_NOMBRE, usuario.getNombreCompleto())
                .claim(CLAIM_ROL, usuario.getRol())
                .setIssuedAt(ahora)
                .setExpiration(new Date(ahora.getTime() + ttlMilis))
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @return la identidad si el token es valido, o {@code null} si la firma no
     *         cuadra, expiro o esta mal formado. Nunca lanza: un token invalido
     *         es trafico normal, no un error del sistema.
     */
    public UsuarioAutenticado verificar(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(clave)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_PERSONA, Integer.class) == null
                            ? null : claims.get(CLAIM_PERSONA, Integer.class).longValue(),
                    claims.get(CLAIM_CONJUNTO, Integer.class) == null
                            ? null : claims.get(CLAIM_CONJUNTO, Integer.class).longValue(),
                    claims.get(CLAIM_DOCUMENTO, String.class),
                    claims.get(CLAIM_NOMBRE, String.class),
                    claims.get(CLAIM_ROL, String.class));

        } catch (Exception ex) {
            log.debug("[auth] token rechazado: {}", ex.getMessage());
            return null;
        }
    }
}
