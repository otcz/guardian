package otcz.guardian.repository.registroAcceso;

import otcz.guardian.entity.registroAcceso.RegistroAcceso;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.utils.ResultadoAcceso;
import otcz.guardian.utils.TipoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    // Historial de acceso por usuarioEntity
    List<RegistroAcceso> findByUsuarioEntityOrderByFechaHoraDesc(UsuarioEntity usuarioEntity);

    // Historial de acceso por invitado
    List<RegistroAcceso> findByInvitadoOrderByFechaHoraDesc(Invitado invitado);

    // Historial de acceso por vehículo
    List<RegistroAcceso> findByVehiculoOrderByFechaHoraDesc(Vehiculo vehiculo);

    // Historial de accesos por resultado
    List<RegistroAcceso> findByResultado(ResultadoAcceso resultado);

    // Accesos dentro de un rango de fechas
    List<RegistroAcceso> findByFechaHoraBetween(LocalDateTime start, LocalDateTime end);

    // Accesos de un usuarioEntity con resultado específico
    List<RegistroAcceso> findByUsuarioEntityAndResultado(UsuarioEntity usuarioEntity, ResultadoAcceso resultado);

    // Accesos de un invitado con tipo de acceso
    List<RegistroAcceso> findByInvitadoAndTipo(Invitado invitado, TipoAcceso tipo);

    // Validar QR en tiempo real (usuarioEntity o invitado)
    @Query("SELECT r FROM RegistroAcceso r WHERE (r.usuarioEntity.id = :usuarioId OR r.invitado.id = :invitadoId) AND r.resultado = otcz.guardian.utils.ResultadoAcceso.AUTORIZADO AND r.fechaHora BETWEEN :inicio AND :fin")
    List<RegistroAcceso> validarAcceso(Long usuarioId, Long invitadoId, LocalDateTime inicio, LocalDateTime fin);
}
