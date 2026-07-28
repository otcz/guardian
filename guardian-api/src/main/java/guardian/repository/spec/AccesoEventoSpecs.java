package guardian.repository.spec;

import guardian.entity.acceso.GdAccesoEvento;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

/**
 * Filtros combinables del historico de accesos.
 *
 * <p><b>Por que Specifications y no un {@code @Query} con {@code :param IS NULL}.</b>
 * Ese patron se compila a un SQL donde Postgres no puede deducir el tipo del
 * parametro cuando llega nulo, y revienta en tiempo de ejecucion con
 * "could not determine data type of parameter". Aca cada filtro nulo
 * simplemente no se agrega al arbol de la consulta, asi que el problema no
 * existe.</p>
 *
 * <p>Devolver {@code null} desde un metodo es intencional:
 * {@code Specification.and(null)} lo ignora, que es justo lo que queremos para
 * un filtro no informado.</p>
 */
public final class AccesoEventoSpecs {

    private AccesoEventoSpecs() {
    }

    public static Specification<GdAccesoEvento> delConjunto(Long conjuntoId) {
        return (root, query, cb) -> cb.equal(root.get("conjuntoId"), conjuntoId);
    }

    public static Specification<GdAccesoEvento> desde(Date desde) {
        return desde == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaEvento"), desde);
    }

    public static Specification<GdAccesoEvento> hasta(Date hasta) {
        return hasta == null ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaEvento"), hasta);
    }

    public static Specification<GdAccesoEvento> deCasa(Long casaId) {
        return casaId == null ? null
                : (root, query, cb) -> cb.equal(root.get("casa").get("id"), casaId);
    }

    public static Specification<GdAccesoEvento> conResultado(String resultado) {
        return (resultado == null || resultado.trim().isEmpty()) ? null
                : (root, query, cb) -> cb.equal(root.get("resultado"), resultado);
    }
}
