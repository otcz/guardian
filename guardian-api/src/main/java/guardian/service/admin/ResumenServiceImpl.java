package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.PresenciaResponse;
import guardian.dto.admin.ResumenResponse;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.service.acceso.AccesoService;
import guardian.service.acceso.PresenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ResumenServiceImpl implements ResumenService {

    private static final int ULTIMOS_MOVIMIENTOS = 8;

    private final GdCasaRepository casaRepository;
    private final GdPersonaRepository personaRepository;
    private final GdVehiculoRepository vehiculoRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final PresenciaService presenciaService;
    private final AccesoService accesoService;

    @Override
    @Transactional(readOnly = true)
    public ResumenResponse resumen(Long conjuntoId) {
        PresenciaResponse presencia = presenciaService.conteo(conjuntoId);

        // "Hoy" en la zona del servidor: es la misma zona de la porteria fisica.
        Date inicioDia = Date.from(LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());

        // Los ultimos movimientos van SIN filtro de fecha: en una manana
        // tranquila el tablero mostraria una lista vacia aunque anoche hubiera
        // actividad, y eso se lee como "no funciona".
        Page<AccesoEventoResponse> ultimos = accesoService.buscarEventos(
                conjuntoId, null, null, null, null, PageRequest.of(0, ULTIMOS_MOVIMIENTOS));
        Page<AccesoEventoResponse> hoy = accesoService.buscarEventos(
                conjuntoId, inicioDia, null, null, null, PageRequest.of(0, 1));
        Page<AccesoEventoResponse> denegadosHoy = accesoService.buscarEventos(
                conjuntoId, inicioDia, null, null, Codigos.RESULTADO_DENEGADO, PageRequest.of(0, 1));

        return ResumenResponse.builder()
                .adentro(presencia.getAdentro())
                .afuera(presencia.getAfuera())
                .casasActivas(casaRepository.countByConjuntoIdAndActivo(conjuntoId, Codigos.SI))
                .casasTotal(casaRepository.countByConjuntoId(conjuntoId))
                .personasActivas(personaRepository.countByConjuntoIdAndActivo(conjuntoId, Codigos.SI))
                .personasTotal(personaRepository.countByConjuntoId(conjuntoId))
                .vehiculosActivos(vehiculoRepository.countByConjuntoIdAndActivo(conjuntoId, Codigos.SI))
                .vehiculosTotal(vehiculoRepository.countByConjuntoId(conjuntoId))
                .usuariosActivos(usuarioRepository.contarPorConjuntoYActivo(conjuntoId, Codigos.SI))
                .usuariosTotal(usuarioRepository.contarPorConjunto(conjuntoId))
                .eventosHoy(hoy.getTotalElements())
                .denegadosHoy(denegadosHoy.getTotalElements())
                .permitidosHoy(hoy.getTotalElements() - denegadosHoy.getTotalElements())
                .ultimosMovimientos(ultimos.getContent())
                .build();
    }
}
