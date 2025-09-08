package otcz.guardian.service.usuario.invitado;

import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.usuario.Usuario;
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
    public List<Invitado> listarInvitadosPorUsuario(Usuario usuario) {
        return invitadoRepository.findByUsuario(usuario);
    }

    @Override
    public List<Invitado> listarInvitadosActivosPorUsuario(Usuario usuario) {
        return invitadoRepository.findByUsuarioAndEstado(usuario, EstadoInvitacion.ACTIVA);
    }

    @Override
    public List<Invitado> listarInvitadosValidos(LocalDateTime fecha) {
        return invitadoRepository.findByEstadoAndFechaInicioBeforeAndFechaFinAfter(
                EstadoInvitacion.ACTIVA, fecha, fecha
        );
    }
}
