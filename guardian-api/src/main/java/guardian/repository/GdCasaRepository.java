package guardian.repository;

import guardian.entity.conjunto.GdCasa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GdCasaRepository extends JpaRepository<GdCasa, Long> {

    Optional<GdCasa> findByConjuntoIdAndIdentificador(Long conjuntoId, String identificador);

    List<GdCasa> findByConjuntoIdOrderByIdentificadorAsc(Long conjuntoId);

    boolean existsByConjuntoIdAndIdentificador(Long conjuntoId, String identificador);

    long countByConjuntoId(Long conjuntoId);

    long countByConjuntoIdAndActivo(Long conjuntoId, String activo);
}
