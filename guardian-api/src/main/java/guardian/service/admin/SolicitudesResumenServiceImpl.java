package guardian.service.admin;

import guardian.dto.admin.ConteoSolicitudesResponse;
import guardian.service.acceso.InvitacionAprobacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolicitudesResumenServiceImpl implements SolicitudesResumenService {

    private final SolicitudCasaAdminService solicitudCasaAdminService;
    private final SolicitudVehiculoAdminService solicitudVehiculoAdminService;
    private final SolicitudHogarAdminService solicitudHogarAdminService;
    private final InvitacionAprobacionService invitacionAprobacionService;

    @Override
    @Transactional(readOnly = true)
    public ConteoSolicitudesResponse conteo(Long conjuntoId) {
        long casas = solicitudCasaAdminService.cuantasPendientes(conjuntoId);
        long vehiculos = solicitudVehiculoAdminService.cuantasPendientes(conjuntoId);
        long hogar = solicitudHogarAdminService.cuantasPendientes(conjuntoId);
        long invitados = invitacionAprobacionService.cuantasPendientes(conjuntoId);

        return ConteoSolicitudesResponse.builder()
                .casas(casas)
                .vehiculos(vehiculos)
                .hogar(hogar)
                .invitados(invitados)
                .pendientes(casas + vehiculos + hogar + invitados)
                .build();
    }
}
