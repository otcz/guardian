package guardian.repository;

import guardian.entity.acceso.GdAccesoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface GdAccesoEventoRepository
        extends JpaRepository<GdAccesoEvento, Long>, JpaSpecificationExecutor<GdAccesoEvento> {

    /**
     * Ultimo evento permitido de la persona, sin ventana de tiempo: define si
     * esta ADENTRO (ultimo sentido = E) o AFUERA. De aca sale tanto el sentido
     * del proximo registro como la regla de que quien ya entro no puede volver
     * a entrar.
     */
    Optional<GdAccesoEvento> findFirstByPersonaIdAndResultadoOrderByFechaEventoDesc(
            Long personaId, String resultado);

    /**
     * Lecturas recientes de la misma credencial. Sirve para el anti-rebote: el
     * guardia que escanea dos veces por nervios no puede generar una entrada y
     * una salida fantasma.
     */
    List<GdAccesoEvento> findByCredencialIdAndFechaEventoAfterOrderByFechaEventoDesc(
            Long credencialId, Date desde);

    // ── Espejo de lo anterior para INVITADOS (evento sin persona). ───────────

    Optional<GdAccesoEvento> findFirstByInvitacionIdAndResultadoOrderByFechaEventoDesc(
            Long invitacionId, String resultado);

    List<GdAccesoEvento> findByInvitacionIdAndFechaEventoAfterOrderByFechaEventoDesc(
            Long invitacionId, Date desde);

    /**
     * El ultimo paso de un VEHICULO. Dice si el carro esta en el conjunto, que
     * no es lo mismo que si su dueno lo esta: el carro lo saca quien lo
     * necesite ese dia.
     */
    Optional<GdAccesoEvento> findFirstByVehiculoIdAndResultadoOrderByFechaEventoDesc(
            Long vehiculoId, String resultado);

    /**
     * Invitados cuyo ultimo evento permitido es una entrada: estan adentro.
     * Con piso de fecha: un invitado que salio a pie sin escanear quedaria
     * "adentro" para siempre y el conteo de evacuacion se volveria mentira.
     * El piso no aplica a estaAdentroInvitado — quien de verdad siga adentro
     * despues de la ventana igual puede registrar su salida.
     */
    @Query("SELECT COUNT(DISTINCT e.invitacion.id) FROM GdAccesoEvento e "
            + "WHERE e.conjuntoId = :conjuntoId "
            + "AND e.resultado = :permitido "
            + "AND e.sentido = :entrada "
            + "AND e.invitacion IS NOT NULL "
            + "AND e.fechaEvento > :piso "
            + "AND e.fechaEvento = (SELECT MAX(e2.fechaEvento) FROM GdAccesoEvento e2 "
            + "                     WHERE e2.invitacion = e.invitacion AND e2.resultado = :permitido)")
    long contarInvitadosAdentro(@Param("conjuntoId") Long conjuntoId,
                                @Param("permitido") String permitido,
                                @Param("entrada") String entrada,
                                @Param("piso") Date piso);

    /**
     * Cuantas personas estan adentro: aquellas cuyo ULTIMO evento permitido es
     * una entrada. La subconsulta correlacionada es aceptable con el volumen de
     * un conjunto residencial; si algun dia duele, se materializa el estado en
     * una columna.
     */
    @Query("SELECT COUNT(DISTINCT e.persona.id) FROM GdAccesoEvento e "
            + "WHERE e.conjuntoId = :conjuntoId "
            + "AND e.resultado = :permitido "
            + "AND e.sentido = :entrada "
            + "AND e.persona IS NOT NULL "
            + "AND e.fechaEvento = (SELECT MAX(e2.fechaEvento) FROM GdAccesoEvento e2 "
            + "                     WHERE e2.persona = e.persona AND e2.resultado = :permitido)")
    long contarAdentro(@Param("conjuntoId") Long conjuntoId,
                       @Param("permitido") String permitido,
                       @Param("entrada") String entrada);

    /**
     * QUIENES estan adentro o afuera, no cuantos.
     *
     * <p>Misma regla que el conteo —el ULTIMO evento permitido de cada
     * persona— pero devolviendo el evento, que ya trae el nombre y la casa
     * copiados. La porteria lo usa para responder "quien falta por salir" sin
     * abrir la bitacora.</p>
     *
     * <p>Con {@code sentido = 'E'} son los que estan adentro; con {@code 'S'},
     * los que estan afuera. Una sola consulta para los dos: son la misma
     * pregunta mirada al derecho y al reves.</p>
     */
    @Query("SELECT e FROM GdAccesoEvento e "
            + "WHERE e.conjuntoId = :conjuntoId "
            + "AND e.resultado = :permitido "
            + "AND e.sentido = :sentido "
            + "AND e.persona IS NOT NULL "
            + "AND e.fechaEvento = (SELECT MAX(e2.fechaEvento) FROM GdAccesoEvento e2 "
            + "                     WHERE e2.persona = e.persona AND e2.resultado = :permitido) "
            + "ORDER BY e.fechaEvento DESC")
    List<GdAccesoEvento> quienesEstan(@Param("conjuntoId") Long conjuntoId,
                                      @Param("permitido") String permitido,
                                      @Param("sentido") String sentido);

    /** Cuanto se ha registrado por una porteria. El panel lo muestra antes de apagarla. */
    long countByPuntoAccesoId(Long puntoAccesoId);

    // ── Desvinculacion para la eliminacion fisica (solo administrador). ──────
    // Los eventos NUNCA se borran: la bitacora conserva nombre/documento/placa
    // copiados, asi que anular la FK no pierde informacion de auditoria.

    @Modifying
    @Query("UPDATE GdAccesoEvento e SET e.persona = null WHERE e.persona.id = :personaId")
    int desvincularPersona(@Param("personaId") Long personaId);

    @Modifying
    @Query("UPDATE GdAccesoEvento e SET e.guardia = null WHERE e.guardia.id = :personaId")
    int desvincularGuardia(@Param("personaId") Long personaId);

    @Modifying
    @Query("UPDATE GdAccesoEvento e SET e.credencial = null WHERE e.credencial.id IN "
            + "(SELECT c.id FROM GdCredencialQr c WHERE c.persona.id = :personaId)")
    int desvincularCredencialesDe(@Param("personaId") Long personaId);

    @Modifying
    @Query("UPDATE GdAccesoEvento e SET e.vehiculo = null WHERE e.vehiculo.id = :vehiculoId")
    int desvincularVehiculo(@Param("vehiculoId") Long vehiculoId);

    @Modifying
    @Query("UPDATE GdAccesoEvento e SET e.invitacion = null WHERE e.invitacion.id IN "
            + "(SELECT i.id FROM GdInvitacion i WHERE i.anfitrion.id = :personaId)")
    int desvincularInvitacionesDeAnfitrion(@Param("personaId") Long personaId);

    @Modifying
    @Query("UPDATE GdAccesoEvento e SET e.invitacion = null WHERE e.invitacion.id = :invitacionId")
    int desvincularInvitacion(@Param("invitacionId") Long invitacionId);

    // La busqueda con filtros opcionales vive en
    // guardian.repository.spec.AccesoEventoSpecs — ver ahi por que no es un @Query.
}
