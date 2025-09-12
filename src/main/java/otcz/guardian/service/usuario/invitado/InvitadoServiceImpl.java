package otcz.guardian.service.usuario.invitado;

import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.repository.usuario.invitado.InvitadoRepository;
import otcz.guardian.utils.EstadoInvitacion;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvitadoServiceImpl implements InvitadoService {

    private final InvitadoRepository invitadoRepository;

    public InvitadoServiceImpl(InvitadoRepository invitadoRepository) {
        this.invitadoRepository = invitadoRepository;
    }

    @Override
    public Invitado crearInvitado(Invitado invitado) {
        return invitadoRepository.save(invitado);
    }

    @Override
    public Invitado actualizarInvitado(Invitado invitado) {
        return invitadoRepository.save(invitado);
    }

    @Override
    public void eliminarInvitado(Long id) {
        invitadoRepository.deleteById(id);
    }

    @Override
    public Optional<Invitado> obtenerPorCodigoQR(String codigoQR) {
        return invitadoRepository.findByCodigoQR(codigoQR);
    }

    @Override
    public List<Invitado> listarInvitadosPorUsuario(UsuarioEntity usuarioEntity) {
        return invitadoRepository.findByUsuarioEntity(usuarioEntity);
    }

    @Override
    public List<Invitado> listarInvitadosActivosPorUsuario(UsuarioEntity usuarioEntity) {
        return invitadoRepository.findByUsuarioEntityAndEstado(usuarioEntity, EstadoInvitacion.ACTIVA);
    }

    @Override
    public List<Invitado> listarInvitadosValidos(LocalDateTime fecha) {
        return invitadoRepository.findByEstadoAndFechaInicioBeforeAndFechaFinAfter(
                EstadoInvitacion.ACTIVA, fecha, fecha
        );
    }
}
