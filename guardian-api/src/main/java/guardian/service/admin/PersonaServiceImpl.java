package guardian.service.admin;

import guardian.constant.ApiEndpoint;
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
import guardian.repository.GdCodigoHogarRepository;
import guardian.repository.GdInvitacionRepository;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.repository.spec.PersonaSpecs;
import guardian.security.Autoridad;
import guardian.security.UsuarioAutenticado;
import guardian.service.acceso.CredencialQrService;
import guardian.util.EdadUtil;
import guardian.util.CorreoUtil;
import guardian.util.FotoUrlUtil;
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
import java.util.regex.Pattern;

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
    private final GdInvitacionRepository invitacionRepository;
    private final GdCodigoHogarRepository codigoHogarRepository;
    private final CredencialQrService credencialQrService;
    private final ParametroService parametroService;
    private final PasswordEncoder passwordEncoder;
    private final guardian.service.foto.FotoStorageService fotoStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<PersonaResponse> buscar(UsuarioAutenticado ejecutor, String texto, Pageable pageable) {
        Specification<GdPersona> filtro = Specification
                .where(PersonaSpecs.delConjunto(ejecutor.getConjuntoId()))
                .and(PersonaSpecs.exceptoLaPropia(ejecutor.getPersonaId()))
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
    public PersonaResponse obtener(Long id, UsuarioAutenticado ejecutor) {
        return mapear(obtenerEntidad(id, ejecutor));
    }

    @Override
    @Transactional
    public PersonaRegistrada crear(PersonaRequest request, UsuarioAutenticado ejecutor) {
        // Mayusculas SIEMPRE: el login compara sin distinguirlas, pero la
        // unicidad en base si distingue. Sin normalizar, "abc" y "ABC" serian
        // dos personas y el login con cualquiera de las dos seria ambiguo.
        String documento = request.getDocumento().trim().toUpperCase();

        // Unicidad GLOBAL, no por sede: la misma cedula no puede existir dos
        // veces aunque los conjuntos sean distintos.
        if (personaRepository.findByDocumento(documento).isPresent()) {
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

        exigirCasaCoherenteConElRol(request);

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
     * mismo paso — habilitada, con la clave inicial y cambio forzado, igual
     * que cualquier cuenta creada desde el panel de usuarios.
     */
    private void crearCuentaSiCorresponde(GdPersona persona, PersonaRequest request,
                                          UsuarioAutenticado ejecutor) {
        if (request.getRolUsuario() == null || request.getRolUsuario().trim().isEmpty()) {
            return;
        }
        // El ROL se valida PRIMERO. Una regla de completitud —falta el correo—
        // no puede colarse delante de una de seguridad: si lo hiciera, un
        // intento de escalar a SUPER_ADMIN sin correo respondería "escribe el
        // correo", y el segundo intento, ya con correo, encontraria la puerta
        // que este chequeo debia cerrar.
        //
        // La via menos obvia de escalada: el alta de persona acepta el rol en
        // el mismo request y crea la cuenta de un tiron. Quien blinde solo
        // UsuarioServiceImpl deja esta puerta abierta.
        Autoridad.exigirRolAsignablePor(ejecutor, request.getRolUsuario());
        parametroService.exigirCodigoValido(Codigos.GRUPO_ROL, request.getRolUsuario());

        // Sin correo la cuenta nace sin salida: quien olvide su clave solo
        // podra recuperarla llamando a la administracion. Se exige aca y no en
        // el DTO porque la mayoria de personas del conjunto NO tienen cuenta
        // —los ninos, los que solo pasan por la porteria— y a esas el correo no
        // les hace falta.
        if (CorreoUtil.normalizar(request.getEmail()) == null) {
            throw GuardianException.solicitudInvalida(
                    MensajesGlobales.CORREO_REQUERIDO_CON_CUENTA);
        }

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(persona);
        usuario.setRol(request.getRolUsuario());
        usuario.setClaveHash(passwordEncoder.encode(Codigos.CLAVE_INICIAL));
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setActivo(Codigos.SI);
        usuario.setUsuarioCreador(ejecutor.getDocumento());
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public PersonaResponse actualizar(Long id, PersonaRequest request, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor);
        String documento = request.getDocumento().trim().toUpperCase();

        personaRepository.findByDocumento(documento)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw GuardianException.conflicto(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);
                });

        // La foto de antes, ANTES de pisarla: si cambio, su archivo queda
        // huerfano en disco y se sirve publicamente por su nombre.
        String fotoAnterior = persona.getFotoUrl();

        persona.setDocumento(documento);
        aplicar(persona, request);
        persona.setUsuarioModificador(ejecutor.getDocumento());

        GdPersona guardada = personaRepository.save(persona);
        vincularCasa(guardada, request, ejecutor);

        // El borrado real ocurre al confirmar la transaccion — ver
        // LocalFotoStorageServiceImpl.alConfirmar().
        fotoStorageService.eliminarReemplazada(fotoAnterior, guardada.getFotoUrl());

        return mapear(guardada);
    }

    @Override
    @Transactional
    public PersonaResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor);
        persona.setActivo(activa ? Codigos.SI : Codigos.NO);
        persona.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] persona id={} activo={} por={}", id, persona.getActivo(),
                ejecutor.getDocumento());
        return mapear(personaRepository.save(persona));
    }

    @Override
    @Transactional
    public String emitirCredencial(Long id, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor);
        return credencialQrService.construirPayload(
                credencialQrService.emitirPermanente(persona, ejecutor.getDocumento()));
    }

    @Override
    @Transactional
    public byte[] credencialPng(Long id, UsuarioAutenticado ejecutor, int tamanoPx) {
        GdPersona persona = obtenerEntidad(id, ejecutor);

        GdCredencialQr credencial = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .orElseThrow(() -> GuardianException.solicitudInvalida(
                        MensajesGlobales.SIN_CREDENCIAL));

        return credencialQrService.renderizarPng(
                credencialQrService.construirPayload(credencial), tamanoPx);
    }

    @Override
    @Transactional
    public void revocarCredencial(Long id, UsuarioAutenticado ejecutor) {
        GdPersona persona = obtenerEntidad(id, ejecutor);

        GdCredencialQr credencial = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .orElseThrow(() -> GuardianException.solicitudInvalida(
                        MensajesGlobales.SIN_CREDENCIAL));

        credencialQrService.revocar(credencial.getId(), ejecutor.getDocumento());
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioAutenticado ejecutor) {
        Autoridad.exigirSuperAdmin(ejecutor);

        GdPersona persona = obtenerEntidad(id, ejecutor);

        // Eliminar la propia persona dejaria al admin con una sesion valida
        // apuntando a un usuario inexistente, y potencialmente sin ningun
        // administrador en el conjunto.
        if (persona.getId().equals(ejecutor.getPersonaId())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.NO_ELIMINARSE_A_SI_MISMO);
        }

        // Orden importa: primero se anulan las FK de la bitacora (que nunca se
        // borra), despues las filas dependientes, al final la persona.
        eventoRepository.desvincularCredencialesDe(id);
        eventoRepository.desvincularInvitacionesDeAnfitrion(id);
        eventoRepository.desvincularPersona(id);
        eventoRepository.desvincularGuardia(id);
        credencialRepository.deleteByPersonaId(id);
        invitacionRepository.deleteByAnfitrionId(id);
        // El codigo de hogar NO se borra: es el historial de quien invito a
        // quien. Solo pierde el enlace a una persona que ya no existe, sea
        // como titular que lo genero o como familiar que lo uso.
        codigoHogarRepository.desvincularTitular(id);
        codigoHogarRepository.desvincularPersonaRegistrada(id);
        residenteCasaRepository.deleteByPersonaId(id);
        usuarioRepository.deleteByPersonaId(id);
        personaRepository.delete(persona);

        // La foto tambien se va: el archivo es publico por nombre UUID y
        // dejarlo huerfano en disco mantendria accesible el dato mas sensible
        // de una persona que pidio ser eliminada.
        fotoStorageService.eliminarPorUrl(persona.getFotoUrl());

        log.warn("[admin] persona ELIMINADA id={} documento={} por={}",
                id, persona.getDocumento(), ejecutor.getDocumento());
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resuelve una persona del panel.
     *
     * <p>Si no esta en la lista, tampoco se puede operar por id: el ejecutor
     * queda fuera con el MISMO 404 que una persona de otra sede. Esconderla de
     * la tabla y dejar el endpoint abierto seria una cortina, no una regla —
     * bastaria un PUT a mano para inactivarse o borrarse.</p>
     */
    private GdPersona obtenerEntidad(Long id, UsuarioAutenticado ejecutor) {
        if (id.equals(ejecutor.getPersonaId())) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return obtenerEntidad(id, ejecutor.getConjuntoId());
    }

    private GdPersona obtenerEntidad(Long id, Long conjuntoId) {
        GdPersona persona = personaRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!persona.getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return persona;
    }

    private void aplicar(GdPersona persona, PersonaRequest request) {
        if (!FotoUrlUtil.esValida(request.getFotoUrl())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.FOTO_URL_INVALIDA);
        }
        // El tipo identifica el documento fisico que la porteria va a comparar:
        // CC de un adulto, TI de un menor, pasaporte de un extranjero.
        String tipo = request.getTipoDocumento() == null
                || request.getTipoDocumento().trim().isEmpty()
                ? Codigos.TIPO_DOCUMENTO_CC
                : request.getTipoDocumento().trim().toUpperCase();
        parametroService.exigirCodigoValido(Codigos.GRUPO_TIPO_DOCUMENTO, tipo);
        persona.setTipoDocumento(tipo);
        persona.setNombres(request.getNombres().trim());
        persona.setApellidos(request.getApellidos().trim());
        persona.setFechaNacimiento(request.getFechaNacimiento());
        persona.setFotoUrl(request.getFotoUrl());
        // Ni telefono ni correo se exigen unicos, y es a proposito: son datos
        // de contacto, no llaves. La recuperacion de clave busca por
        // DOCUMENTO y solo lee el correo para enviar, asi que compartirlo no
        // extravia nada — y compartirlo es lo normal en una casa (el hijo
        // menor usa el correo de la mama, la pareja comparte un celular). Exigir
        // uno distinto por persona obligaria a inventar datos que nadie revisa.
        persona.setTelefono(request.getTelefono());
        // Minusculas y sin espacios: "Ana@X.com" no encontraria la fila
        // guardada como "ana@x.com".
        persona.setEmail(CorreoUtil.normalizar(request.getEmail()));
    }

    /**
     * Crea o actualiza el vinculo con la casa. Si el request no trae casa se
     * deja como esta: un guardia o un administrador externo no vive en ninguna
     * unidad y eso es valido.
     */
    /**
     * Un guardia no vive en el conjunto: trabaja en el.
     *
     * <p>La pantalla ya deja de ofrecer la casa al elegir GUARDIA, pero la
     * regla no puede vivir solo alli: el mismo endpoint lo llama cualquiera con
     * el token, y una casa colada en el alta de un guardia lo mete en un nucleo
     * familiar — con los vehiculos y los invitados de esa casa detras.</p>
     */
    private void exigirCasaCoherenteConElRol(PersonaRequest request) {
        if (request.getCasaId() != null
                && Codigos.ROL_GUARDIA.equals(request.getRolUsuario())) {
            throw GuardianException.conflicto(MensajesGlobales.GUARDIA_SIN_CASA);
        }
    }

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

        // Un nucleo por persona: si ya vive en OTRA casa, primero hay que
        // sacarla de esa. Sin esta regla la misma cedula aparece en dos
        // familias y la porteria no sabe de que casa son sus vehiculos.
        residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI)
                .filter(otro -> !otro.getCasa().getId().equals(casa.getId()))
                .ifPresent(otro -> {
                    throw GuardianException.conflicto(MensajesGlobales.PERSONA_YA_EN_UNA_CASA);
                });

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

        // Y AL REVES: una casa NO puede quedarse sin titular. Cubre los dos
        // caminos por los que pasaba: el primero que entra a una casa vacia
        // —si no es el titular, nadie puede administrar esa familia y todos
        // ven un boton que no funciona— y el titular al que le cambian el
        // parentesco cuando no hay ningun otro.
        if (!Codigos.PARENTESCO_TITULAR.equals(request.getParentesco())) {
            boolean hayOtroTitular = residenteCasaRepository
                    .findFirstByCasaIdAndParentescoAndActivo(
                            casa.getId(), Codigos.PARENTESCO_TITULAR, Codigos.SI)
                    .filter(otro -> !otro.getPersona().getId().equals(persona.getId()))
                    .isPresent();
            if (!hayOtroTitular) {
                throw GuardianException.conflicto(MensajesGlobales.CASA_NECESITA_TITULAR);
            }
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
        return FotoUrlUtil.tieneFoto(persona.getFotoUrl());
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
                .tipoDocumento(persona.getTipoDocumento())
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
                .bloqueado(persona.getBloqueado())
                .motivoBloqueo(persona.getMotivoBloqueo())
                .casaId(vinculo.map(v -> v.getCasa().getId()).orElse(null))
                .casaIdentificador(vinculo.map(v -> v.getCasa().getIdentificador()).orElse(null))
                .parentesco(vinculo.map(GdResidenteCasa::getParentesco).orElse(null))
                .tieneCredencial(conCredencial)
                .usuarioId(usuario.map(GdUsuario::getId).orElse(null))
                .rol(usuario.map(GdUsuario::getRol).orElse(null))
                .usuarioActivo(usuario.map(GdUsuario::getActivo).orElse(null))
                .usuarioBloqueado(usuario.map(GdUsuario::getBloqueado).orElse(null))
                .usuarioMotivoBloqueo(usuario.map(GdUsuario::getMotivoBloqueo).orElse(null))
                .usuarioUltimoIngreso(usuario.map(GdUsuario::getFechaUltimoIngreso).orElse(null))
                .build();
    }
}
