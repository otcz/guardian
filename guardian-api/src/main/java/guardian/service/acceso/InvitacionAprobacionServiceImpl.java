package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.InvitacionPendienteResponse;
import guardian.entity.acceso.GdInvitacion;
import guardian.exception.GuardianException;
import guardian.repository.GdInvitacionRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitacionAprobacionServiceImpl implements InvitacionAprobacionService {

    private final GdInvitacionRepository invitacionRepository;
    private final guardian.service.notificacion.NotificacionService notificacionService;

    @Override
    @Transactional(readOnly = true)
    public List<InvitacionPendienteResponse> pendientes(Long conjuntoId) {
        return invitacionRepository
                .listarPorConjuntoYEstadoAprobacion(conjuntoId, Codigos.SOLICITUD_PENDIENTE)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long cuantasPendientes(Long conjuntoId) {
        return invitacionRepository.countByConjuntoIdAndEstadoAprobacion(
                conjuntoId, Codigos.SOLICITUD_PENDIENTE);
    }

    @Override
    @Transactional
    public InvitacionPendienteResponse aprobar(Long id, UsuarioAutenticado ejecutor) {
        GdInvitacion invitacion = pendienteDeLaSede(id, ejecutor.getConjuntoId());

        invitacion.setEstadoAprobacion(Codigos.SOLICITUD_APROBADA);
        invitacion.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] invitacion {} aprobada por {}", id, ejecutor.getDocumento());

        notificacionService.invitacionResuelta(invitacion, true);
        return mapear(invitacionRepository.save(invitacion));
    }

    @Override
    @Transactional
    public InvitacionPendienteResponse rechazar(Long id, String motivo,
                                                UsuarioAutenticado ejecutor) {
        GdInvitacion invitacion = pendienteDeLaSede(id, ejecutor.getConjuntoId());

        invitacion.setEstadoAprobacion(Codigos.SOLICITUD_RECHAZADA);
        invitacion.setMotivoRechazo(motivo == null || motivo.trim().isEmpty()
                ? null : motivo.trim());
        invitacion.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] invitacion {} rechazada por {}", id, ejecutor.getDocumento());

        notificacionService.invitacionResuelta(invitacion, false);
        return mapear(invitacionRepository.save(invitacion));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdInvitacion pendienteDeLaSede(Long id, Long conjuntoId) {
        GdInvitacion invitacion = invitacionRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!invitacion.getConjuntoId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        if (!Codigos.SOLICITUD_PENDIENTE.equals(invitacion.getEstadoAprobacion())) {
            throw GuardianException.conflicto(MensajesGlobales.SOLICITUD_NO_PENDIENTE);
        }
        return invitacion;
    }

    private InvitacionPendienteResponse mapear(GdInvitacion invitacion) {
        return InvitacionPendienteResponse.builder()
                .id(invitacion.getId())
                .nombreInvitado(invitacion.getNombreInvitado())
                .documentoInvitado(invitacion.getDocumentoInvitado())
                .casaIdentificador(invitacion.getCasa().getIdentificador())
                .anfitrionNombre(invitacion.getAnfitrion().getNombreCompleto())
                .vigenciaDesde(invitacion.getVigenciaDesde())
                .vigenciaHasta(invitacion.getVigenciaHasta())
                .estado(invitacion.getEstadoAprobacion())
                .fechaSolicitud(invitacion.getFechaCreacion())
                .build();
    }
}
