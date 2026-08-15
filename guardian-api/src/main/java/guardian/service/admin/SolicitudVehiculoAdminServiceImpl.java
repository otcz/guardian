package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.SolicitudVehiculoAdminResponse;
import guardian.dto.admin.VehiculoRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.entity.persona.GdPersona;
import guardian.entity.vehiculo.GdSolicitudVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdSolicitudVehiculoRepository;
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
public class SolicitudVehiculoAdminServiceImpl implements SolicitudVehiculoAdminService {

    private final GdSolicitudVehiculoRepository solicitudRepository;
    private final VehiculoService vehiculoService;
    private final EtiquetaCatalogoService etiquetaCatalogoService;

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudVehiculoAdminResponse> pendientes(Long conjuntoId) {
        return solicitudRepository
                .listarPorConjuntoYEstado(conjuntoId, Codigos.SOLICITUD_PENDIENTE)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long cuantasPendientes(Long conjuntoId) {
        return solicitudRepository.contarPorConjuntoYEstado(
                conjuntoId, Codigos.SOLICITUD_PENDIENTE);
    }

    @Override
    @Transactional
    public SolicitudVehiculoAdminResponse aprobar(Long id, UsuarioAutenticado ejecutor) {
        GdSolicitudVehiculo solicitud = pendienteDeLaSede(id, ejecutor.getConjuntoId());

        VehiculoRequest alta = new VehiculoRequest();
        alta.setCasaId(solicitud.getCasa().getId());
        alta.setPlaca(solicitud.getPlaca());
        alta.setTipo(solicitud.getTipo());
        alta.setMarca(solicitud.getMarca());
        alta.setColor(solicitud.getColor());
        // La foto pasa tal cual: es la misma que el administrador acaba de mirar
        // para decidir, y volver a pedirla despues dejaria al vehiculo recien
        // autorizado sin la imagen que la porteria va a comparar.
        alta.setFotoUrl(solicitud.getFotoUrl());

        // Por el service de vehiculos y no con un save directo: es el que sabe
        // que la placa es unica en todo el sistema y que la casa tiene que ser
        // de esta sede. Si entre pedir y aprobar alguien registro esa misma
        // placa, esto revienta aca —con el conflicto delante del administrador,
        // que es quien puede resolverlo— y la solicitud sigue pendiente.
        VehiculoResponse vehiculo = vehiculoService.crear(alta, ejecutor);

        solicitud.setEstado(Codigos.SOLICITUD_APROBADA);
        solicitud.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] solicitud de vehiculo {} aprobada: placa {} queda en {} "
                        + "(vehiculoId={}, por {})",
                id, solicitud.getPlaca(), solicitud.getCasa().getIdentificador(),
                vehiculo.getId(), ejecutor.getDocumento());
        return mapear(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional
    public SolicitudVehiculoAdminResponse rechazar(Long id, String motivo,
                                                   UsuarioAutenticado ejecutor) {
        GdSolicitudVehiculo solicitud = pendienteDeLaSede(id, ejecutor.getConjuntoId());

        solicitud.setEstado(Codigos.SOLICITUD_RECHAZADA);
        // El motivo lo lee el residente: sin el, un rechazo es una puerta
        // cerrada sin explicacion y la persona vuelve a pedir lo mismo.
        solicitud.setMotivoRechazo(motivo == null || motivo.trim().isEmpty()
                ? null : motivo.trim());
        solicitud.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] solicitud de vehiculo {} (placa {}) rechazada por {}",
                id, solicitud.getPlaca(), ejecutor.getDocumento());
        return mapear(solicitudRepository.save(solicitud));
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * La solicitud, dentro de la sede del token y todavia sin resolver.
     *
     * <p>Lo segundo importa tanto como lo primero: sin ese filtro, dos
     * administradores mirando la misma bandeja podrian aprobar y rechazar la
     * misma fila, y la segunda decision pisaria a la primera en silencio.</p>
     */
    private GdSolicitudVehiculo pendienteDeLaSede(Long id, Long conjuntoId) {
        GdSolicitudVehiculo solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!solicitud.getCasa().getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        if (!Codigos.SOLICITUD_PENDIENTE.equals(solicitud.getEstado())) {
            throw GuardianException.conflicto(MensajesGlobales.SOLICITUD_NO_PENDIENTE);
        }
        return solicitud;
    }

    private SolicitudVehiculoAdminResponse mapear(GdSolicitudVehiculo solicitud) {
        GdPersona quien = solicitud.getSolicitante();
        return SolicitudVehiculoAdminResponse.builder()
                .id(solicitud.getId())
                .placa(solicitud.getPlaca())
                .tipoNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_TIPO_VEHICULO, solicitud.getTipo()))
                .marcaNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_MARCA_VEHICULO, solicitud.getMarca()))
                .colorNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_COLOR_VEHICULO, solicitud.getColor()))
                .fotoUrl(solicitud.getFotoUrl())
                .casaId(solicitud.getCasa().getId())
                .casaIdentificador(solicitud.getCasa().getIdentificador())
                .solicitante(quien.getNombres() + " " + quien.getApellidos())
                .documento(quien.getDocumento())
                .estado(solicitud.getEstado())
                .fechaSolicitud(solicitud.getFechaCreacion())
                .build();
    }
}
