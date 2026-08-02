package guardian.repository;

import guardian.entity.auth.GdCodigoRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GdCodigoRecuperacionRepository
        extends JpaRepository<GdCodigoRecuperacion, Long> {

    /**
     * El ultimo codigo emitido para ese usuario. Solo el ultimo importa: pedir
     * uno nuevo apaga los anteriores.
     */
    Optional<GdCodigoRecuperacion> findFirstByUsuarioIdOrderByIdDesc(Long usuarioId);

    /**
     * Apaga los codigos vivos antes de emitir uno nuevo.
     *
     * <p>Sin esto, un codigo viejo seguiria sirviendo diez minutos despues de
     * que su duena pidiera otro — y quien pide otro suele hacerlo justamente
     * porque sospecha que el primero se le fue a la persona equivocada.</p>
     */
    @Modifying
    @Query("UPDATE GdCodigoRecuperacion c SET c.activo = 'N' "
            + "WHERE c.usuario.id = :usuarioId AND c.activo = 'S' AND c.fechaUso IS NULL")
    int revocarVigentesDe(@Param("usuarioId") Long usuarioId);

    /**
     * Suma un intento fallido.
     *
     * <p>UPDATE directo y no {@code save()} de la entidad: esto corre en una
     * transaccion aparte —ver ContadorIntentosRecuperacion— y una entidad
     * cargada en la transaccion de afuera llegaria alli desprendida.</p>
     */
    @Modifying
    @Query("UPDATE GdCodigoRecuperacion c SET c.intentos = c.intentos + 1 WHERE c.id = :id")
    int sumarIntento(@Param("id") Long id);
}
