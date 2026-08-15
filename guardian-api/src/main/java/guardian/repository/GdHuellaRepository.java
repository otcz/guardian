package guardian.repository;

import guardian.entity.persona.GdHuella;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdHuellaRepository extends JpaRepository<GdHuella, Long> {

    List<GdHuella> findByPersonaId(Long personaId);

    Optional<GdHuella> findByPersonaIdAndDedo(Long personaId, String dedo);

    long countByPersonaId(Long personaId);

    void deleteByPersonaId(Long personaId);

    /**
     * TODAS las huellas de la sede, para el cotejo 1:N.
     *
     * <p>Se traen enteras porque el cotejo biometrico no se puede hacer en SQL:
     * no es una comparacion de igualdad sino un calculo de similitud que hace
     * el algoritmo plantilla por plantilla. La base no puede ayudar aca.</p>
     *
     * <p><b>Y por eso esto no escala solo.</b> Con doscientos residentes son
     * doscientas comparaciones por persona que llega — instantaneo. Con
     * decenas de miles habria que pasar a un motor biometrico con indice, o
     * pedir el documento primero para convertirlo en un cotejo 1:1. Se anota
     * aca para que el dia que duela se sepa donde mirar.</p>
     *
     * <p>Solo de quien puede pasar: una huella de alguien inactivo o bloqueado
     * no tiene por que entrar siquiera al cotejo.</p>
     */
    @Query("SELECT h FROM GdHuella h JOIN FETCH h.persona p "
            + "WHERE p.conjunto.id = :conjuntoId "
            + "AND p.activo = 'S' AND p.bloqueado = 'N' "
            + "AND h.activo = 'S'")
    List<GdHuella> operativasDeLaSede(@Param("conjuntoId") Long conjuntoId);
}
