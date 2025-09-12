package otcz.guardian.repository.usuario.invitado;

import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.utils.EstadoInvitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitadoRepository extends JpaRepository<Invitado, Long> {

    // Buscar invitado por código QR
    Optional<Invitado> findByCodigoQR(String codigoQR);

    // Listar invitados activos de un usuario
    List<Invitado> findByUsuarioEntityAndEstado(UsuarioEntity usuarioEntity, EstadoInvitacion estado);

    // Listar invitados válidos en una fecha concreta
    List<Invitado> findByEstadoAndFechaInicioBeforeAndFechaFinAfter(EstadoInvitacion estado,
                                                                    LocalDateTime fechaInicio,
                                                                    LocalDateTime fechaFin);

    // Listar invitados por usuario
    List<Invitado> findByUsuarioEntity(UsuarioEntity usuarioEntity);
}
