package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.admin.PersonaResponse;
import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.repository.spec.PersonaSpecs;
import guardian.security.UsuarioAutenticado;
import guardian.service.acceso.CredencialQrService;
import guardian.util.EdadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonaServiceImpl implements PersonaService {

    private final GdPersonaRepository personaRepository;
    private final GdConjuntoRepository conjuntoRepository;
    private final GdCasaRepository casaRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdCredencialQrRepository credencialRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final GdAccesoEventoRepository eventoRepository;
    private final CredencialQrService credencialQrService;
    private final ParametroService parametroService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<PersonaResponse> buscar(Long conjuntoId, String texto, Pageable pageable) {
        Specification<GdPersona> filtro = Specification
                .where(PersonaSpecs.delConjunto(conjuntoId))
                .and(PersonaSpecs.coincideCon(texto));

        return personaRepository.findAll(filtro, ordenar(pageable)).map(this::mapear);
    }

    /** Alfabetico por apellido, que es como se busca a alguien en una lista. */
    private Pageable ordenar(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "apellidos", "nombres"));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse obtener(Long id, Long conjuntoId) {
        return mapear(obtenerEntidad(id, conjuntoId));
    }

    @Override
    @Transactional
    public PersonaRegistrada crear(PersonaRequest request, UsuarioAutenticado ejecutor) {
        String documento = request.getDocumento().trim();

        if (personaRepository.existsByConjuntoIdAndDocumento(ejecutor.getConjuntoId(), documento)) {
            throw GuardianException.conflicto(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);
        }

        GdConjunto conjunto = conjuntoRepository.findById(ejecutor.getConjuntoId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        GdPersona persona = new GdPersona();
        persona.setConjunto(conjunto);
        persona.setDocumento(documento);
        aplicar(persona, request);
        persona.setActivo(Codigos.SI);
        persona.setUsuarioCreador(ejecutor.getDocumento());

        GdPersona guardada = personaRepository.save(persona);
        vincularCasa(guardada, request, ejecutor);
        crearCuentaSiCorresponde(guardada, request, ejecutor);

        // La credencial se emite sola cuando ya hay foto. Si falta, no se
        // interrumpe el alta: la persona queda registrada y el administrador
        // sube la foto despues. Bloquear el registro entero por una foto haria
        // que se pierda todo lo digitado.
        String payload = null;
        if (tieneFoto(guardada)) {
            payload = credencialQrService.construirPayload(
                    credencialQrService.emitirPermanente(guardada, ejecutor.getDocumento()));
        }

        log.info("[admin] persona creada id={} documento={} conFoto={} conCuenta={}",
                guardada.getId(), documento, payload != null, request.getRolUsuario() != null);

        return new PersonaRegistrada(mapear(guardada), payload);
    }

    /**
     * La "tarjeta completa": si el alta trae rol, la cuenta se crea en el
     * mismo paso. Nace INACTIVA y con clave = documento + cambio forzado,
     * igual que cualquier cuenta creada desde el panel de usuarios.
     */
    private void crearCuentaSiCorresponde(GdPersona persona, PersonaRequest request,
                                          UsuarioAutenticado ejecutor) {
        if (request.getRolUsuario() == null || request.getRolUsuario().trim().isEmpty()) {
            return;
        }
        parametroService.exigirCodigoValido(Codigos.GRUPO_ROL, request.getRolUsuario());

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(persona);
        usuario.setRol(request.getRolUsuario());
        usuario.setClaveHash(passwordEncoder.encode(persona.getDocumento()));
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setActivo(Codigos.NO);
        usuario.setUsuarioCreador(ejecutor.getDocumento());
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public PersonaResponse actualizar(Long id, PersonaRequest request, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor.getConjuntoId());
        String documento = request.getDocumento().trim();

        personaRepository.findByConjuntoIdAndDocumento(ejecutor.getConjuntoId(), documento)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw GuardianException.conflicto(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);
                });

        persona.setDocumento(documento);
        aplicar(persona, request);
        persona.setUsuarioModificador(ejecutor.getDocumento());

        GdPersona guardada = personaRepository.save(persona);
        vincularCasa(guardada, request, ejecutor);

        return mapear(guardada);
    }

    @Override
    @Transactional
    public PersonaResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor.getConjuntoId());
        persona.setActivo(activa ? Codigos.SI : Codigos.NO);
        persona.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] persona id={} activo={} por={}", id, persona.getActivo(),
                ejecutor.getDocumento());
        return mapear(personaRepository.save(persona));
    }

    @Override
    @Transactional
    public String emitirCredencial(Long id, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor.getConjuntoId());
        return credencialQrService.construirPayload(
                credencialQrService.emitirPermanente(persona, ejecutor.getDocumento()));
    }

    @Override
    @Transactional
    public byte[] credencialPng(Long id, Long conjuntoId, int tamanoPx) {
        GdPersona persona = obtenerEntidad(id, conjuntoId);

        GdCredencialQr credencial = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .orElseThrow(() -> GuardianException.solicitudInvalida(
                        "Esta persona todavia no tiene credencial."));

        return credencialQrService.renderizarPng(
                credencialQrService.construirPayload(credencial), tamanoPx);
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor.getConjuntoId());

        // Eliminar la propia persona dejaria al admin con una sesion valida
        // apuntando a un usuario inexistente, y potencialmente sin ningun
        // administrador en el conjunto.
        if (persona.getId().equals(ejecutor.getPersonaId())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.NO_ELIMINARSE_A_SI_MISMO);
        }

        // Orden importa: primero se anulan las FK de la bitacora (que nunca se
        // borra), despues las filas dependientes, al final la persona.
        eventoRepository.desvincularCredencialesDe(id);
        eventoRepository.desvincularPersona(id);
        eventoRepository.desvincularGuardia(id);
        credencialRepository.deleteByPersonaId(id);
        residenteCasaRepository.deleteByPersonaId(id);
        usuarioRepository.deleteByPersonaId(id);
        personaRepository.delete(persona);

        log.warn("[admin] persona ELIMINADA id={} documento={} por={}",
                id, persona.getDocumento(), ejecutor.getDocumento());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdPersona obtenerEntidad(Long id, Long conjuntoId) {
        GdPersona persona = personaRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!persona.getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return persona;
    }

    private void aplicar(GdPersona persona, PersonaRequest request) {
        persona.setNombres(request.getNombres().trim());
        persona.setApellidos(request.getApellidos().trim());
        persona.setFechaNacimiento(request.getFechaNacimiento());
        persona.setFotoUrl(request.getFotoUrl());
        persona.setTelefono(request.getTelefono());
        persona.setEmail(request.getEmail());
    }

    /**
     * Crea o actualiza el vinculo con la casa. Si el request no trae casa se
     * deja como esta: un guardia o un administrador externo no vive en ninguna
     * unidad y eso es valido.
     */
    private void vincularCasa(GdPersona persona, PersonaRequest request,
                              UsuarioAutenticado ejecutor) {
        if (request.getCasaId() == null) {
            return;
        }

        parametroService.exigirCodigoValido(Codigos.GRUPO_PARENTESCO, request.getParentesco());

        GdCasa casa = casaRepository.findById(request.getCasaId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!casa.getConjunto().getId().equals(ejecutor.getConjuntoId())) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }

        // Una casa tiene un solo titular. Dos titulares dejarian sin respuesta
        // la pregunta de a quien se le notifica o quien autoriza invitados en F2.
        if (Codigos.PARENTESCO_TITULAR.equals(request.getParentesco())) {
            residenteCasaRepository
                    .findFirstByCasaIdAndParentescoAndActivo(
                            casa.getId(), Codigos.PARENTESCO_TITULAR, Codigos.SI)
                    .filter(otro -> !otro.getPersona().getId().equals(persona.getId()))
                    .ifPresent(otro -> {
                        throw GuardianException.conflicto(MensajesGlobales.TITULAR_YA_EXISTE);
                    });
        }

        GdResidenteCasa vinculo = residenteCasaRepository
                .findByPersonaIdAndCasaId(persona.getId(), casa.getId())
                .orElseGet(GdResidenteCasa::new);

        vinculo.setPersona(persona);
        vinculo.setCasa(casa);
        vinculo.setParentesco(request.getParentesco());
        vinculo.setActivo(Codigos.SI);

        if (vinculo.getId() == null) {
            vinculo.setUsuarioCreador(ejecutor.getDocumento());
        } else {
            vinculo.setUsuarioModificador(ejecutor.getDocumento());
        }
        residenteCasaRepository.save(vinculo);
    }

    private boolean tieneFoto(GdPersona persona) {
        return persona.getFotoUrl() != null && !persona.getFotoUrl().trim().isEmpty();
    }

    private PersonaResponse mapear(GdPersona persona) {
        Optional<GdResidenteCasa> vinculo = residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI);

        Optional<GdUsuario> usuario = usuarioRepository.findByPersonaId(persona.getId());

        boolean conCredencial = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .isPresent();

        return PersonaResponse.builder()
                .id(persona.getId())
                .documento(persona.getDocumento())
                .nombres(persona.getNombres())
                .apellidos(persona.getApellidos())
                .nombreCompleto(persona.getNombreCompleto())
                .fechaNacimiento(persona.getFechaNacimiento())
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .fotoUrl(persona.getFotoUrl())
                .telefono(persona.getTelefono())
                .email(persona.getEmail())
                .activo(persona.getActivo())
                .casaId(vinculo.map(v -> v.getCasa().getId()).orElse(null))
                .casaIdentificador(vinculo.map(v -> v.getCasa().getIdentificador()).orElse(null))
                .parentesco(vinculo.map(GdResidenteCasa::getParentesco).orElse(null))
                .tieneCredencial(conCredencial)
                .rol(usuario.map(GdUsuario::getRol).orElse(null))
                .usuarioActivo(usuario.map(GdUsuario::getActivo).orElse(null))
                .build();
    }
}
