package guardian.service.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import guardian.entity.parametro.GdParametro;
import guardian.repository.GdParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class EtiquetaCatalogoServiceImpl implements EtiquetaCatalogoService {

    private final GdParametroRepository parametroRepository;

    /** grupo -> (codigo -> texto visible). Son decenas de filas, no miles. */
    private final Cache<String, Map<String, String>> porGrupo;

    public EtiquetaCatalogoServiceImpl(GdParametroRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
        // El cache se invalida explicitamente al editar el catalogo; el
        // vencimiento por tiempo es solo la red de seguridad por si un dia
        // alguien escribe en GD_PARAMETRO sin pasar por ParametroService.
        this.porGrupo = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(32)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String etiqueta(String grupo, String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }
        return porGrupo.get(grupo, this::cargar).getOrDefault(codigo, codigo);
    }

    @Override
    public void invalidar(String grupo) {
        porGrupo.invalidate(grupo);
    }

    /**
     * Incluye las inactivas. Una marca que el administrador apago sigue
     * guardada en los vehiculos que ya la declararon, y esos hay que poder
     * mostrarlos.
     */
    private Map<String, String> cargar(String grupo) {
        List<GdParametro> parametros = parametroRepository.findByGrupoOrderByOrdenAsc(grupo);
        Map<String, String> etiquetas = new HashMap<>();
        for (GdParametro parametro : parametros) {
            etiquetas.put(parametro.getCodigo(), parametro.getValor());
        }
        return etiquetas;
    }
}
