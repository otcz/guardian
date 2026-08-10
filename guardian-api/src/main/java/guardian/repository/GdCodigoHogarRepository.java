package guardian.repository;

import guardian.entity.persona.GdCodigoHogar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GdCodigoHogarRepository extends JpaRepository<GdCodigoHogar, Long> {

    Optional<GdCodigoHogar> findByCodigo(String codigo);

    /**
     * El ultimo codigo de una casa, usado o no. La pantalla del titular
     * necesita mostrarle el que tiene vivo, y si ya se uso, quien lo uso.
     */
    Optional<GdCodigoHogar> findFirstByCasaIdOrderByIdDesc(Long casaId);

    /**
     * El codigo NO se borra al eliminar a su titular: sigue siendo el
     * historial de quien invito a quien. Solo pierde el enlace a una fila que
     * ya no existe, igual que un evento de la bitacora.
     */
    @Modifying
    @Query("UPDATE GdCodigoHogar c SET c.titular = null WHERE c.titular.id = :personaId")
    int desvincularTitular(@Param("personaId") Long personaId);

    @Modifying
    @Query("UPDATE GdCodigoHogar c SET c.personaRegistrada = null "
            + "WHERE c.personaRegistrada.id = :personaId")
    int desvincularPersonaRegistrada(@Param("personaId") Long personaId);
}
