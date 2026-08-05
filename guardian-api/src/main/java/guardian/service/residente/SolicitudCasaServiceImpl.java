package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.residente.CasaDisponibleResponse;
import guardian.dto.residente.SolicitudCasaRequest;
import guardian.dto.residente.SolicitudCasaResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdSolicitudCasa;
import guardian.exception.GuardianException;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdSolicitudCasaRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.ParametroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudCasaServiceImpl implements SolicitudCasaService {

    private final GdSolicitudCasaRepository solicitudRepository;
    private final GdCasaRepository casaRepository;
    private final GdPersonaRepository personaRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final ParametroService parametroService;

    @Override
    @Transactional(readOnly = true)
    public List<CasaDisponibleResponse> casasDisponibles(UsuarioAutenticado solicitante) {
        exigirQueNoTengaCasa(solicitante.getPersonaId());

        return casaRepository.findByConjuntoIdOrderByIdentificadorAsc(solicitante.getConjuntoId())
                .stream()
                // Una casa apagada por la administracion no se ofrece: pedir
                // entrar a ella terminaria en un rechazo que nadie entiende.
                .filter(GdCasa::estaActivo)
                .map(casa -> CasaDisponibleResponse.builder()
                        .id(casa.getId())
                        .identificador(casa.getIdentificador())
                        .tieneTitular(tieneTitular(casa.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolicitudCasaResponse> miSolicitud(UsuarioAutenticado solicitante) {
        return solicitudRepository
                .findFirstByPersonaIdOrderByIdDesc(solicitante.getPersonaId())
                .map(this::mapear);
    }

    @Override
    @Transactional
    public SolicitudCasaResponse solicitar(SolicitudCasaRequest request,
                                           UsuarioAutenticado solicitante) {
        exigirQueNoTengaCasa(solicitante.getPersonaId());
        parametroService.exigirCodigoValido(Codigos.GRUPO_PARENTESCO, request.getParentesco());

        GdCasa casa = casaRepository.findById(request.getCasaId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        // La casa del token manda: sin esto alguien podria pedir entrar a una
        // casa de otra sede mandando un id ajeno.
        if (!casa.getConjunto().getId().equals(solicitante.getConjuntoId())) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }

        // Se valida ya, aunque la aprobacion lo vuelva a mirar: pedir ser
        // titular de una casa que ya lo tiene solo llenaria la bandeja de
        // solicitudes que nacen para ser rechazadas.
        exigirParentescoCoherente(casa.getId(), request.getParentesco());

        GdSolicitudCasa solicitud = solicitudRepository
                .findFirstByPersonaIdAndEstadoOrderByIdDesc(
                        solicitante.getPersonaId(), Codigos.SOLICITUD_PENDIENTE)
                // Una sola pendiente por persona: la que ya tenia se reescribe,
                // porque cambiar de opinion antes de que le respondan no puede
                // dejarle dos pedidos vivos al administrador.
                .orElseGet(GdSolicitudCasa::new);

        GdPersona persona = personaRepository.findById(solicitante.getPersonaId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        solicitud.setPersona(persona);
        solicitud.setCasa(casa);
        solicitud.setParentesco(request.getParentesco());
        solicitud.setEstado(Codigos.SOLICITUD_PENDIENTE);
        solicitud.setMotivoRechazo(null);
        solicitud.setActivo(Codigos.SI);
        solicitud.setBloqueado(Codigos.NO);
        if (solicitud.getId() == null) {
            solicitud.setUsuarioCreador(solicitante.getDocumento());
        } else {
            solicitud.setUsuarioModificador(solicitante.getDocumento());
        }

        GdSolicitudCasa guardada = solicitudRepository.save(solicitud);
        log.info("[residente] {} pide entrar a la casa {} como {}",
                solicitante.getDocumento(), casa.getIdentificador(), request.getParentesco());
        return mapear(guardada);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Solo pide casa quien no tiene. Con una casa activa esta pantalla no
     * deberia abrirse siquiera, y dejar pedir otra seria una forma de mudarse
     * sin que nadie lo autorice.
     */
    private void exigirQueNoTengaCasa(Long personaId) {
        if (residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(personaId, Codigos.SI)
                .isPresent()) {
            throw GuardianException.conflicto(MensajesGlobales.YA_TIENE_CASA);
        }
    }

    private boolean tieneTitular(Long casaId) {
        return residenteCasaRepository
                .findFirstByCasaIdAndParentescoAndActivo(
                        casaId, Codigos.PARENTESCO_TITULAR, Codigos.SI)
                .isPresent();
    }

    /**
     * Una casa vacia solo admite un TITULAR —si no, nace sin quien administre la
     * familia— y una con titular no admite un segundo.
     */
    private void exigirParentescoCoherente(Long casaId, String parentesco) {
        boolean esTitular = Codigos.PARENTESCO_TITULAR.equals(parentesco);
        boolean yaHay = tieneTitular(casaId);

        if (esTitular && yaHay) {
            throw GuardianException.conflicto(MensajesGlobales.TITULAR_YA_EXISTE);
        }
        if (!esTitular && !yaHay) {
            throw GuardianException.conflicto(MensajesGlobales.CASA_NECESITA_TITULAR);
        }
    }

    private SolicitudCasaResponse mapear(GdSolicitudCasa solicitud) {
        return SolicitudCasaResponse.builder()
                .id(solicitud.getId())
                .casaId(solicitud.getCasa().getId())
                .casaIdentificador(solicitud.getCasa().getIdentificador())
                .parentesco(solicitud.getParentesco())
                .estado(solicitud.getEstado())
                .motivoRechazo(solicitud.getMotivoRechazo())
                .fechaSolicitud(solicitud.getFechaCreacion())
                .build();
    }
}
