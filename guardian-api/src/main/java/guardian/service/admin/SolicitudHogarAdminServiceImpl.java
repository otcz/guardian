package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.SolicitudHogarAdminResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdCodigoHogar;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdSolicitudHogar;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdCodigoHogarRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdSolicitudHogarRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudHogarAdminServiceImpl implements SolicitudHogarAdminService {

    private final GdSolicitudHogarRepository solicitudRepository;
    private final GdCodigoHogarRepository codigoRepository;
    private final GdPersonaRepository personaRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final guardian.service.notificacion.NotificacionService notificacionService;

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudHogarAdminResponse> pendientes(Long conjuntoId) {
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
    public SolicitudHogarAdminResponse aprobar(Long id, UsuarioAutenticado ejecutor) {
        GdSolicitudHogar solicitud = pendienteDeLaSede(id, ejecutor.getConjuntoId());
        String documento = solicitud.getDocumento();

        // Entre pedir y aprobar pudo pasar cualquier cosa: otro camino de alta
        // pudo registrar ese mismo documento mientras la solicitud esperaba.
        if (personaRepository.findByDocumento(documento).isPresent()) {
            throw GuardianException.conflicto(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);
        }

        GdCodigoHogar codigo = solicitud.getCodigo();
        GdCasa casa = codigo.getCasa();

        GdPersona persona = new GdPersona();
        persona.setConjunto(casa.getConjunto());
        persona.setTipoDocumento(solicitud.getTipoDocumento() == null
                || solicitud.getTipoDocumento().trim().isEmpty()
                ? Codigos.TIPO_DOCUMENTO_CC
                : solicitud.getTipoDocumento());
        persona.setDocumento(documento);
        persona.setNombres(solicitud.getNombres());
        persona.setApellidos(solicitud.getApellidos());
        persona.setFechaNacimiento(solicitud.getFechaNacimiento());
        persona.setTelefono(solicitud.getTelefono());
        persona.setEmail(solicitud.getEmail());
        persona.setActivo(Codigos.SI);
        persona.setBloqueado(Codigos.NO);
        persona.setUsuarioCreador(ejecutor.getDocumento());

        GdPersona guardada = personaRepository.save(persona);

        GdResidenteCasa enLaCasa = new GdResidenteCasa();
        enLaCasa.setPersona(guardada);
        enLaCasa.setCasa(casa);
        enLaCasa.setParentesco(solicitud.getParentesco());
        enLaCasa.setActivo(Codigos.SI);
        enLaCasa.setUsuarioCreador(ejecutor.getDocumento());
        residenteCasaRepository.save(enLaCasa);

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(guardada);
        usuario.setRol(Codigos.ROL_RESIDENTE);
        usuario.setClaveHash(passwordEncoder.encode(Codigos.CLAVE_INICIAL));
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setActivo(Codigos.SI);
        usuario.setBloqueado(Codigos.NO);
        usuario.setUsuarioCreador(ejecutor.getDocumento());
        usuarioRepository.save(usuario);

        // El codigo queda quemado solo ahora: es el momento en que la persona
        // de verdad existe, no el de la solicitud.
        codigo.setPersonaRegistrada(guardada);
        codigo.setUsuarioModificador(ejecutor.getDocumento());
        codigoRepository.save(codigo);

        solicitud.setEstado(Codigos.SOLICITUD_APROBADA);
        solicitud.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] solicitud de hogar {} aprobada: {} entra a {} como {} (por {})",
                id, documento, casa.getIdentificador(), solicitud.getParentesco(),
                ejecutor.getDocumento());

        notificacionService.solicitudHogarResuelta(solicitud, true);
        return mapear(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional
    public SolicitudHogarAdminResponse rechazar(Long id, String motivo,
                                                UsuarioAutenticado ejecutor) {
        GdSolicitudHogar solicitud = pendienteDeLaSede(id, ejecutor.getConjuntoId());

        solicitud.setEstado(Codigos.SOLICITUD_RECHAZADA);
        solicitud.setMotivoRechazo(motivo == null || motivo.trim().isEmpty()
                ? null : motivo.trim());
        solicitud.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] solicitud de hogar {} rechazada por {}", id, ejecutor.getDocumento());

        notificacionService.solicitudHogarResuelta(solicitud, false);
        return mapear(solicitudRepository.save(solicitud));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdSolicitudHogar pendienteDeLaSede(Long id, Long conjuntoId) {
        GdSolicitudHogar solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!solicitud.getCodigo().getCasa().getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        if (!Codigos.SOLICITUD_PENDIENTE.equals(solicitud.getEstado())) {
            throw GuardianException.conflicto(MensajesGlobales.SOLICITUD_NO_PENDIENTE);
        }
        return solicitud;
    }

    private SolicitudHogarAdminResponse mapear(GdSolicitudHogar solicitud) {
        GdPersona titular = solicitud.getCodigo().getTitular();
        return SolicitudHogarAdminResponse.builder()
                .id(solicitud.getId())
                .nombreCompleto(solicitud.getNombres() + " " + solicitud.getApellidos())
                .documento(solicitud.getDocumento())
                .casaIdentificador(solicitud.getCodigo().getCasa().getIdentificador())
                .parentesco(solicitud.getParentesco())
                .titularNombre(titular != null
                        ? titular.getNombreCompleto() : MensajesGlobales.TITULAR_ELIMINADO)
                .estado(solicitud.getEstado())
                .fechaSolicitud(solicitud.getFechaCreacion())
                .build();
    }
}
