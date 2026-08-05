package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.SolicitudCasaAdminResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdSolicitudCasa;
import guardian.exception.GuardianException;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdSolicitudCasaRepository;
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
public class SolicitudCasaAdminServiceImpl implements SolicitudCasaAdminService {

    private final GdSolicitudCasaRepository solicitudRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCasaAdminResponse> pendientes(Long conjuntoId) {
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
    public SolicitudCasaAdminResponse aprobar(Long id, UsuarioAutenticado ejecutor) {
        GdSolicitudCasa solicitud = pendienteDeLaSede(id, ejecutor.getConjuntoId());
        GdPersona persona = solicitud.getPersona();
        GdCasa casa = solicitud.getCasa();

        // Entre pedir y aprobar pudo pasar cualquier cosa. Se vuelve a mirar.
        if (residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI)
                .isPresent()) {
            throw GuardianException.conflicto(MensajesGlobales.PERSONA_YA_EN_UNA_CASA);
        }

        boolean pideTitular = Codigos.PARENTESCO_TITULAR.equals(solicitud.getParentesco());
        boolean yaHayTitular = residenteCasaRepository
                .findFirstByCasaIdAndParentescoAndActivo(
                        casa.getId(), Codigos.PARENTESCO_TITULAR, Codigos.SI)
                .isPresent();

        if (pideTitular && yaHayTitular) {
            throw GuardianException.conflicto(MensajesGlobales.TITULAR_YA_EXISTE);
        }
        if (!pideTitular && !yaHayTitular) {
            throw GuardianException.conflicto(MensajesGlobales.CASA_NECESITA_TITULAR);
        }

        GdResidenteCasa vinculo = residenteCasaRepository
                .findByPersonaIdAndCasaId(persona.getId(), casa.getId())
                .orElseGet(GdResidenteCasa::new);
        vinculo.setPersona(persona);
        vinculo.setCasa(casa);
        vinculo.setParentesco(solicitud.getParentesco());
        vinculo.setActivo(Codigos.SI);
        vinculo.setBloqueado(Codigos.NO);
        if (vinculo.getId() == null) {
            vinculo.setUsuarioCreador(ejecutor.getDocumento());
        } else {
            vinculo.setUsuarioModificador(ejecutor.getDocumento());
        }
        residenteCasaRepository.save(vinculo);

        solicitud.setEstado(Codigos.SOLICITUD_APROBADA);
        solicitud.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] solicitud {} aprobada: {} entra a {} como {} (por {})",
                id, persona.getDocumento(), casa.getIdentificador(),
                solicitud.getParentesco(), ejecutor.getDocumento());
        return mapear(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional
    public SolicitudCasaAdminResponse rechazar(Long id, String motivo,
                                               UsuarioAutenticado ejecutor) {
        GdSolicitudCasa solicitud = pendienteDeLaSede(id, ejecutor.getConjuntoId());

        solicitud.setEstado(Codigos.SOLICITUD_RECHAZADA);
        // El motivo lo lee el residente: sin el, un rechazo es una puerta
        // cerrada sin explicacion y la persona vuelve a pedir lo mismo.
        solicitud.setMotivoRechazo(motivo == null || motivo.trim().isEmpty()
                ? null : motivo.trim());
        solicitud.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] solicitud {} rechazada por {}", id, ejecutor.getDocumento());
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
    private GdSolicitudCasa pendienteDeLaSede(Long id, Long conjuntoId) {
        GdSolicitudCasa solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!solicitud.getCasa().getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        if (!Codigos.SOLICITUD_PENDIENTE.equals(solicitud.getEstado())) {
            throw GuardianException.conflicto(MensajesGlobales.SOLICITUD_NO_PENDIENTE);
        }
        return solicitud;
    }

    private SolicitudCasaAdminResponse mapear(GdSolicitudCasa solicitud) {
        GdPersona persona = solicitud.getPersona();
        return SolicitudCasaAdminResponse.builder()
                .id(solicitud.getId())
                .personaId(persona.getId())
                .nombreCompleto(persona.getNombres() + " " + persona.getApellidos())
                .documento(persona.getDocumento())
                .casaIdentificador(solicitud.getCasa().getIdentificador())
                .parentesco(solicitud.getParentesco())
                .estado(solicitud.getEstado())
                .fechaSolicitud(solicitud.getFechaCreacion())
                .casaConTitular(residenteCasaRepository
                        .findFirstByCasaIdAndParentescoAndActivo(
                                solicitud.getCasa().getId(),
                                Codigos.PARENTESCO_TITULAR, Codigos.SI)
                        .isPresent())
                .build();
    }
}
