package guardian.service.admin;

import guardian.dto.admin.ConteoSolicitudesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolicitudesResumenServiceImpl implements SolicitudesResumenService {

    private final SolicitudCasaAdminService solicitudCasaAdminService;
    private final SolicitudVehiculoAdminService solicitudVehiculoAdminService;

    @Override
    @Transactional(readOnly = true)
    public ConteoSolicitudesResponse conteo(Long conjuntoId) {
        long casas = solicitudCasaAdminService.cuantasPendientes(conjuntoId);
        long vehiculos = solicitudVehiculoAdminService.cuantasPendientes(conjuntoId);

        return ConteoSolicitudesResponse.builder()
                .casas(casas)
                .vehiculos(vehiculos)
                .pendientes(casas + vehiculos)
                .build();
    }
}
