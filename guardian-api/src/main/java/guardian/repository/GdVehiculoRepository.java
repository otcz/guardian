package guardian.repository;

import guardian.entity.vehiculo.GdVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GdVehiculoRepository extends JpaRepository<GdVehiculo, Long> {

    Optional<GdVehiculo> findByConjuntoIdAndPlaca(Long conjuntoId, String placa);

    /** Unicidad GLOBAL: la misma placa no puede estar en dos sedes ni dos casas. */
    Optional<GdVehiculo> findByPlaca(String placa);

    /**
     * Los vehiculos que de verdad pueden salir: encendidos por su casa y no
     * bloqueados por la administracion. Es lo que ve el guardia en la ficha.
     *
     * <p>Tiene que coincidir EXACTAMENTE con las puertas que valida el
     * registro: si la lista ofrece una placa que el registro va a rechazar, el
     * guardia toca y recibe un error — y eso rompe el "maximo un toque" con
     * gente esperando en la porteria.</p>
     */
    @Query("SELECT v FROM GdVehiculo v WHERE v.casa.id = :casaId "
            + "AND v.activo = 'S' AND v.bloqueado = 'N' ORDER BY v.placa ASC")
    List<GdVehiculo> operativosDeLaCasa(@Param("casaId") Long casaId);

    boolean existsByConjuntoIdAndPlaca(Long conjuntoId, String placa);

    List<GdVehiculo> findByCasaIdAndActivoOrderByPlacaAsc(Long casaId, String activo);

    List<GdVehiculo> findByCasaIdOrderByPlacaAsc(Long casaId);

    List<GdVehiculo> findByConjuntoIdOrderByPlacaAsc(Long conjuntoId);

    long countByConjuntoId(Long conjuntoId);

    long countByConjuntoIdAndActivo(Long conjuntoId, String activo);
}
