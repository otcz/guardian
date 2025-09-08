package otcz.guardian.service.registroAcceso;

import otcz.guardian.entity.registroAcceso.RegistroAcceso;
import otcz.guardian.entity.usuario.Usuario;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.repository.registroAcceso.RegistroAccesoRepository;
import otcz.guardian.utils.ResultadoAcceso;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistroAccesoServiceImpl implements RegistroAccesoService {

    private final RegistroAccesoRepository registroAccesoRepository;

    public RegistroAccesoServiceImpl(RegistroAccesoRepository registroAccesoRepository) {
        this.registroAccesoRepository = registroAccesoRepository;
    }

    @Override
    public RegistroAcceso registrarAcceso(RegistroAcceso registroAcceso) {
        return registroAccesoRepository.save(registroAcceso);
    }

    @Override
    public List<RegistroAcceso> historialPorUsuario(Usuario usuario) {
        return registroAccesoRepository.findByUsuarioOrderByFechaHoraDesc(usuario);
    }

    @Override
    public List<RegistroAcceso> historialPorInvitado(Invitado invitado) {
        return registroAccesoRepository.findByInvitadoOrderByFechaHoraDesc(invitado);
    }

    @Override
    public List<RegistroAcceso> historialPorVehiculo(Vehiculo vehiculo) {
        return registroAccesoRepository.findByVehiculoOrderByFechaHoraDesc(vehiculo);
    }

    @Override
    public List<RegistroAcceso> historialPorResultado(ResultadoAcceso resultado) {
        return registroAccesoRepository.findByResultado(resultado);
    }

    @Override
    public List<RegistroAcceso> historialEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        return registroAccesoRepository.findByFechaHoraBetween(inicio, fin);
    }

    @Override
    public List<RegistroAcceso> validarQR(Long usuarioId, Long invitadoId, LocalDateTime inicio, LocalDateTime fin) {
        return registroAccesoRepository.validarAcceso(usuarioId, invitadoId, inicio, fin);
    }
}
