package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.residente.SolicitudVehiculoResponse;
import guardian.dto.residente.VehiculoResidenteRequest;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.vehiculo.GdSolicitudVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdSolicitudVehiculoRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.EtiquetaCatalogoService;
import guardian.service.admin.ParametroService;
import guardian.util.CatalogoVehiculo;
import guardian.util.FotoUrlUtil;
import guardian.util.PlacaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudVehiculoServiceImpl implements SolicitudVehiculoService {

    /** Lo que el residente ve en su pantalla: lo que espera y lo que le negaron. */
    private static final List<String> EN_PANTALLA =
            Arrays.asList(Codigos.SOLICITUD_PENDIENTE, Codigos.SOLICITUD_RECHAZADA);

    private final GdSolicitudVehiculoRepository solicitudRepository;
    private final GdVehiculoRepository vehiculoRepository;
    private final GdPersonaRepository personaRepository;
    private final HogarDelResidente hogar;
    private final ParametroService parametroService;
    private final EtiquetaCatalogoService etiquetaCatalogoService;

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudVehiculoResponse> misSolicitudes(UsuarioAutenticado solicitante) {
        GdCasa casa = hogar.casa(solicitante);

        return solicitudRepository.listarPorCasa(casa.getId(), Codigos.SI, EN_PANTALLA)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SolicitudVehiculoResponse solicitar(VehiculoResidenteRequest request,
                                               UsuarioAutenticado solicitante) {
        GdCasa casa = hogar.casa(solicitante);

        // Igual que con la familia: quien responde por el hogar es el titular.
        // Un hijo ve los carros de su casa, pero no mete uno nuevo.
        hogar.exigirTitular(solicitante, casa, MensajesGlobales.SOLO_TITULAR_VEHICULOS);

        CatalogoVehiculo.exigir(parametroService, request.getTipo(),
                request.getMarca(), request.getColor());

        // Se valida ACA y no solo al aprobar: una URL externa guardada en la
        // solicitud le quedaria delante al administrador como si fuera el carro.
        if (!FotoUrlUtil.esValida(request.getFotoUrl())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.FOTO_URL_INVALIDA);
        }

        String placa = PlacaUtil.normalizar(request.getPlaca());

        // Las dos colisiones posibles, cortadas antes de llenar la bandeja: la
        // placa ya registrada y la placa ya pedida por alguien mas. Al residente
        // se le dice ahora y no dentro de dos dias con un rechazo.
        if (vehiculoRepository.findByPlaca(placa).isPresent()) {
            throw GuardianException.conflicto(MensajesGlobales.PLACA_YA_REGISTRADA);
        }
        solicitudRepository
                .findFirstByPlacaAndEstado(placa, Codigos.SOLICITUD_PENDIENTE)
                .ifPresent(otra -> {
                    throw GuardianException.conflicto(MensajesGlobales.PLACA_YA_SOLICITADA);
                });

        GdPersona persona = personaRepository.findById(solicitante.getPersonaId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        GdSolicitudVehiculo solicitud = new GdSolicitudVehiculo();
        solicitud.setCasa(casa);
        solicitud.setSolicitante(persona);
        solicitud.setPlaca(placa);
        solicitud.setTipo(request.getTipo());
        solicitud.setMarca(CatalogoVehiculo.limpiar(request.getMarca()));
        solicitud.setColor(CatalogoVehiculo.limpiar(request.getColor()));
        solicitud.setFotoUrl(request.getFotoUrl());
        solicitud.setEstado(Codigos.SOLICITUD_PENDIENTE);
        solicitud.setActivo(Codigos.SI);
        solicitud.setBloqueado(Codigos.NO);
        solicitud.setUsuarioCreador(solicitante.getDocumento());

        GdSolicitudVehiculo guardada = solicitudRepository.save(solicitud);
        log.info("[residente] {} pide autorizar la placa {} para la casa {}",
                solicitante.getDocumento(), placa, casa.getIdentificador());
        return mapear(guardada);
    }

    @Override
    @Transactional
    public void descartar(Long id, UsuarioAutenticado solicitante) {
        GdCasa casa = hogar.casa(solicitante);
        hogar.exigirTitular(solicitante, casa, MensajesGlobales.SOLO_TITULAR_VEHICULOS);

        GdSolicitudVehiculo solicitud = solicitudRepository.findById(id)
                .filter(s -> s.getCasa().getId().equals(casa.getId()))
                .orElseThrow(() -> GuardianException.sinPermiso(MensajesGlobales.SOLICITUD_AJENA));

        // Solo lo ya respondido. Descartar una pendiente seria cancelarla, y eso
        // le dejaria al administrador una fila que aprobar sobre algo que el
        // residente cree haber retirado.
        if (!Codigos.SOLICITUD_RECHAZADA.equals(solicitud.getEstado())) {
            throw GuardianException.conflicto(MensajesGlobales.SOLICITUD_NO_RESUELTA);
        }

        // Se apaga, no se borra: el rechazo y su motivo siguen existiendo para
        // quien pregunte despues por que ese carro no entra.
        solicitud.setActivo(Codigos.NO);
        solicitud.setUsuarioModificador(solicitante.getDocumento());
        solicitudRepository.save(solicitud);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private SolicitudVehiculoResponse mapear(GdSolicitudVehiculo solicitud) {
        return SolicitudVehiculoResponse.builder()
                .id(solicitud.getId())
                .placa(solicitud.getPlaca())
                .tipo(solicitud.getTipo())
                .marca(solicitud.getMarca())
                .color(solicitud.getColor())
                .tipoNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_TIPO_VEHICULO, solicitud.getTipo()))
                .marcaNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_MARCA_VEHICULO, solicitud.getMarca()))
                .colorNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_COLOR_VEHICULO, solicitud.getColor()))
                .fotoUrl(solicitud.getFotoUrl())
                .estado(solicitud.getEstado())
                .motivoRechazo(solicitud.getMotivoRechazo())
                .fechaSolicitud(solicitud.getFechaCreacion())
                .build();
    }
}
