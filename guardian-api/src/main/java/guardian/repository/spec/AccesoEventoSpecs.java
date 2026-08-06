package guardian.repository.spec;

import guardian.entity.acceso.GdAccesoEvento;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;
import java.util.List;

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

    /**
     * Filtro de varios valores para una misma columna — el desplegable de la
     * cabecera, donde se marcan uno o varios.
     *
     * <p>Lista vacia devuelve null y no filtra: "ninguno marcado" en un
     * autofiltro significa "todos", no "nada". Si devolviera un IN vacio, la
     * bitacora saldria en blanco apenas alguien abriera un menu.</p>
     */
    public static Specification<GdAccesoEvento> conValoresDe(String campo, List<String> valores) {
        if (valores == null || valores.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(campo).in(valores);
    }

    /** Igual, para la porteria, que se filtra por id y no por texto. */
    public static Specification<GdAccesoEvento> enPorterias(List<Long> porteriaIds) {
        if (porteriaIds == null || porteriaIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("puntoAcceso").get("id").in(porteriaIds);
    }

    /**
     * Busqueda libre sobre lo que el evento COPIO al registrarse: nombre,
     * documento, casa y placa.
     *
     * <p>Sobre las copias y no sobre las tablas vivas a proposito. La bitacora
     * responde por lo que paso ESE dia: si a alguien le cambiaron el apellido
     * o el carro cambio de casa, buscar por el dato de hoy no deberia dejar de
     * encontrar el evento de entonces — y un join contra la persona actual
     * haria justo eso.</p>
     */
    public static Specification<GdAccesoEvento> queDiga(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String patron = "%" + texto.trim().toLowerCase() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("personaNombre")), patron),
                cb.like(cb.lower(root.get("personaDocumento")), patron),
                cb.like(cb.lower(root.get("casaIdentificador")), patron),
                cb.like(cb.lower(root.get("vehiculoPlaca")), patron));
    }
}
