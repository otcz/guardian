package guardian.repository.spec;

import guardian.entity.persona.GdPersona;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de busqueda de personas. Ver la nota de {@link AccesoEventoSpecs}
 * sobre por que no se usa {@code :param IS NULL} dentro de un {@code @Query}.
 */
public final class PersonaSpecs {

    private PersonaSpecs() {
    }

    public static Specification<GdPersona> delConjunto(Long conjuntoId) {
        return (root, query, cb) -> cb.equal(root.get("conjunto").get("id"), conjuntoId);
    }

    /**
     * Un solo cuadro de texto contra documento, nombres y apellidos. Quien
     * administra el conjunto escribe "perez" o "1020" indistintamente y espera
     * que aparezca, sin tener que elegir antes por que campo esta buscando.
     */
    public static Specification<GdPersona> coincideCon(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String patron = "%" + texto.trim().toLowerCase() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombres")), patron),
                cb.like(cb.lower(root.get("apellidos")), patron),
                cb.like(cb.lower(root.get("documento")), patron));
    }
}
