package otcz.guardian.service.registroAcceso;

import otcz.guardian.entity.registroAcceso.RegistroAcceso;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.utils.ResultadoAcceso;
import java.time.LocalDateTime;
import java.util.List;

public interface RegistroAccesoService {

    RegistroAcceso registrarAcceso(RegistroAcceso registroAcceso);

    List<RegistroAcceso> historialPorUsuario(UsuarioEntity usuarioEntity);

    List<RegistroAcceso> historialPorInvitado(Invitado invitado);

    List<RegistroAcceso> historialPorVehiculo(VehiculoEntity vehiculoEntity);

    List<RegistroAcceso> historialPorResultado(ResultadoAcceso resultado);

    List<RegistroAcceso> historialEntreFechas(LocalDateTime inicio, LocalDateTime fin);

    List<RegistroAcceso> validarQR(Long usuarioId, Long invitadoId, LocalDateTime inicio, LocalDateTime fin);
}
