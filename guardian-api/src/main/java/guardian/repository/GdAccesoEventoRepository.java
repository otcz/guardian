package guardian.repository;

import guardian.entity.acceso.GdAccesoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface GdAccesoEventoRepository
        extends JpaRepository<GdAccesoEvento, Long>, JpaSpecificationExecutor<GdAccesoEvento> {

    /**
     * Ultimo evento permitido de la persona. Es lo que deja inferir el sentido
     * del proximo: si la ultima vez entro, ahora sale.
     */
    Optional<GdAccesoEvento> findFirstByPersonaIdAndResultadoAndFechaEventoAfterOrderByFechaEventoDesc(
            Long personaId, String resultado, Date desde);

    /**
     * Lecturas recientes de la misma credencial. Sirve para el anti-rebote: el
     * guardia que escanea dos veces por nervios no puede generar una entrada y
     * una salida fantasma.
     */
    List<GdAccesoEvento> findByCredencialIdAndFechaEventoAfterOrderByFechaEventoDesc(
            Long credencialId, Date desde);

    // La busqueda con filtros opcionales vive en
    // guardian.repository.spec.AccesoEventoSpecs — ver ahi por que no es un @Query.
}
