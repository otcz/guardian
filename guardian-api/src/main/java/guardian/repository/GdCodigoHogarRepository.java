package guardian.repository;

import guardian.entity.persona.GdCodigoHogar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GdCodigoHogarRepository extends JpaRepository<GdCodigoHogar, Long> {

    Optional<GdCodigoHogar> findByCodigo(String codigo);

    /**
     * El ultimo codigo de una casa, usado o no. La pantalla del titular
     * necesita mostrarle el que tiene vivo, y si ya se uso, quien lo uso.
     */
    Optional<GdCodigoHogar> findFirstByCasaIdOrderByIdDesc(Long casaId);
}
