package guardian.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import guardian.entity.persona.GdUsuario;
import guardian.repository.GdUsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Estado fresco del usuario para cada peticion autenticada.
 *
 * <p><b>Por que existe.</b> Un JWT sin estado vive hasta 12 horas: si el
 * administrador deshabilita a un guardia o le cambia el rol, el token viejo
 * seguiria valiendo hasta expirar. Este servicio consulta la base en cada
 * peticion — amortiguado por un cache Caffeine de pocos segundos — para que
 * deshabilitar surta efecto de inmediato y no en el proximo turno.</p>
 *
 * <p>El TTL es el maximo de retraso aceptado entre "el admin deshabilito" y
 * "la sesion murio". Con el default de 60 segundos, el costo en base es a lo
 * sumo un query por usuario activo por minuto.</p>
 */
@Slf4j
@Service
public class EstadoUsuarioService {

    private static final long MAXIMO_USUARIOS_EN_CACHE = 5_000;

    private final GdUsuarioRepository usuarioRepository;
    private final Cache<Long, Optional<EstadoUsuario>> cache;

    public EstadoUsuarioService(
            GdUsuarioRepository usuarioRepository,
            @Value("${guardian.security.estado-cache-segundos}") long ttlSegundos) {
        this.usuarioRepository = usuarioRepository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSegundos, TimeUnit.SECONDS)
                .maximumSize(MAXIMO_USUARIOS_EN_CACHE)
                .build();
    }

    public Optional<EstadoUsuario> estadoDe(Long usuarioId) {
        return cache.get(usuarioId, this::cargar);
    }

    /**
     * Descarta la entrada cacheada. Se invoca cuando el propio backend cambia
     * el estado (cambio de clave, restablecimiento) para que el efecto sea
     * inmediato en vez de esperar el TTL.
     */
    public void invalidar(Long usuarioId) {
        cache.invalidate(usuarioId);
    }

    private Optional<EstadoUsuario> cargar(Long usuarioId) {
        return usuarioRepository.buscarConPersona(usuarioId)
                .map(this::mapear);
    }

    private EstadoUsuario mapear(GdUsuario usuario) {
        // La persona tambien cuenta: inhabilitar a la persona es inhabilitar
        // su acceso al conjunto, y dejarle la app abierta seria incoherente.
        boolean puedeOperar = usuario.estaActivo() && usuario.getPersona().estaActivo();
        return new EstadoUsuario(puedeOperar, usuario.getRol(), usuario.debeCambiarClave());
    }
}
