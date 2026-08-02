package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.residente.CodigoHogarResponse;
import guardian.dto.residente.HogarPublicoResponse;
import guardian.dto.residente.RegistroHogarRequest;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdCodigoHogar;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdCodigoHogarRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.ParametroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoHogarServiceImpl implements CodigoHogarService {

    private static final long MILIS_POR_HORA = 3_600_000L;

    private final GdCodigoHogarRepository codigoRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdPersonaRepository personaRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final ParametroService parametroService;
    private final PasswordEncoder passwordEncoder;

    @Value("${guardian.hogar.horas-vigencia-codigo}")
    private long horasVigencia;

    // ── Del lado del titular ─────────────────────────────────────────────────

    @Override
    @Transactional
    public CodigoHogarResponse generar(UsuarioAutenticado titular) {
        GdCasa casa = miCasaComoTitular(titular);

        // Uno vivo a la vez: si quedaran varios, revocar el que se compartio
        // por error no serviria de nada porque los otros seguirian abiertos.
        codigoRepository.findFirstByCasaIdOrderByIdDesc(casa.getId())
                .filter(anterior -> !anterior.estaUsado())
                .ifPresent(anterior -> {
                    anterior.setActivo(Codigos.NO);
                    codigoRepository.save(anterior);
                });

        GdCodigoHogar codigo = new GdCodigoHogar();
        codigo.setConjuntoId(titular.getConjuntoId());
        codigo.setCasa(casa);
        codigo.setTitular(personaDe(titular.getPersonaId()));
        codigo.setCodigo(UUID.randomUUID().toString());
        codigo.setVigenciaHasta(
                new Date(System.currentTimeMillis() + horasVigencia * MILIS_POR_HORA));
        codigo.setActivo(Codigos.SI);
        codigo.setBloqueado(Codigos.NO);
        codigo.setUsuarioCreador(titular.getDocumento());

        GdCodigoHogar guardado = codigoRepository.save(codigo);
        log.info("[hogar] codigo generado casaId={} por={}", casa.getId(), titular.getDocumento());

        return mapear(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public CodigoHogarResponse vigente(UsuarioAutenticado usuario) {
        GdCasa casa = miCasa(usuario);
        return codigoRepository.findFirstByCasaIdOrderByIdDesc(casa.getId())
                .map(this::mapear)
                .orElse(null);
    }

    @Override
    @Transactional
    public void revocar(UsuarioAutenticado titular) {
        GdCasa casa = miCasaComoTitular(titular);
        codigoRepository.findFirstByCasaIdOrderByIdDesc(casa.getId())
                .filter(c -> !c.estaUsado())
                .ifPresent(c -> {
                    c.setActivo(Codigos.NO);
                    c.setUsuarioModificador(titular.getDocumento());
                    codigoRepository.save(c);
                    log.info("[hogar] codigo revocado casaId={}", casa.getId());
                });
    }

    // ── Del lado de quien se une ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public HogarPublicoResponse consultar(String codigo) {
        GdCodigoHogar vinculo = codigoRepository.findByCodigo(codigo)
                .orElseThrow(() -> GuardianException.noEncontrado(
                        MensajesGlobales.CODIGO_HOGAR_NO_VALIDO));

        return HogarPublicoResponse.builder()
                .conjuntoNombre(vinculo.getCasa().getConjunto().getNombre())
                .casaIdentificador(vinculo.getCasa().getIdentificador())
                .titularNombre(vinculo.getTitular().getNombreCompleto())
                .vigente(vinculo.sirve(new Date()))
                // Las opciones del formulario viajan aca: esa pantalla no tiene
                // sesion y no puede pedir el catalogo por su cuenta.
                .parentescos(parametroService.listarPorGrupo(Codigos.GRUPO_PARENTESCO).stream()
                        .filter(p -> !Codigos.PARENTESCO_TITULAR.equals(p.getCodigo()))
                        .collect(Collectors.toList()))
                .tiposDocumento(parametroService.listarPorGrupo(Codigos.GRUPO_TIPO_DOCUMENTO))
                .build();
    }

    @Override
    @Transactional
    public void registrar(String codigo, RegistroHogarRequest request) {
        GdCodigoHogar vinculo = codigoRepository.findByCodigo(codigo)
                .orElseThrow(() -> GuardianException.noEncontrado(
                        MensajesGlobales.CODIGO_HOGAR_NO_VALIDO));

        // Un mensaje unico para vencido, revocado y ya usado: quien esta al
        // otro lado no necesita distinguirlos y a quien prueba codigos no hay
        // por que decirle cual de los tres acerto.
        if (!vinculo.sirve(new Date())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.CODIGO_HOGAR_NO_VALIDO);
        }

        // TITULAR no: esa casa ya tiene uno y el codigo lo genero el.
        if (Codigos.PARENTESCO_TITULAR.equals(request.getParentesco())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.PARENTESCO_TITULAR_NO);
        }
        parametroService.exigirCodigoValido(Codigos.GRUPO_PARENTESCO, request.getParentesco());

        String documento = request.getDocumento().trim().toUpperCase();
        if (personaRepository.findByDocumento(documento).isPresent()) {
            throw GuardianException.conflicto(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);
        }

        GdCasa casa = vinculo.getCasa();

        GdPersona persona = new GdPersona();
        persona.setConjunto(casa.getConjunto());
        persona.setTipoDocumento(
                request.getTipoDocumento() == null || request.getTipoDocumento().trim().isEmpty()
                        ? Codigos.TIPO_DOCUMENTO_CC
                        : request.getTipoDocumento());
        persona.setDocumento(documento);
        persona.setNombres(request.getNombres().trim());
        persona.setApellidos(request.getApellidos().trim());
        persona.setFechaNacimiento(request.getFechaNacimiento());
        persona.setTelefono(request.getTelefono());
        persona.setActivo(Codigos.SI);
        persona.setBloqueado(Codigos.NO);
        // El auditor deja escrito que entro por un codigo y de quien era, no
        // por la mano del administrador.
        persona.setUsuarioCreador(vinculo.getTitular().getDocumento());

        GdPersona guardada = personaRepository.save(persona);

        GdResidenteCasa enLaCasa = new GdResidenteCasa();
        enLaCasa.setPersona(guardada);
        enLaCasa.setCasa(casa);
        enLaCasa.setParentesco(request.getParentesco());
        enLaCasa.setActivo(Codigos.SI);
        enLaCasa.setUsuarioCreador(vinculo.getTitular().getDocumento());
        residenteCasaRepository.save(enLaCasa);

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(guardada);
        usuario.setRol(Codigos.ROL_RESIDENTE);
        usuario.setClaveHash(passwordEncoder.encode(Codigos.CLAVE_INICIAL));
        usuario.setRequiereCambioClave(Codigos.SI);
        usuario.setActivo(Codigos.SI);
        usuario.setBloqueado(Codigos.NO);
        usuario.setUsuarioCreador(vinculo.getTitular().getDocumento());
        usuarioRepository.save(usuario);

        // Quemar el codigo es lo ULTIMO: si algo de arriba falla, la
        // transaccion revierte y el codigo sigue sirviendo. Al reves, un fallo
        // dejaria al familiar sin cuenta y sin forma de reintentar.
        vinculo.setPersonaRegistrada(guardada);
        vinculo.setUsuarioModificador(documento);
        codigoRepository.save(vinculo);

        log.info("[hogar] {} se registro en la casa {} con el codigo del titular",
                documento, casa.getIdentificador());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private CodigoHogarResponse mapear(GdCodigoHogar codigo) {
        return CodigoHogarResponse.builder()
                .codigo(codigo.getCodigo())
                .vigenciaHasta(codigo.getVigenciaHasta())
                .vigente(codigo.sirve(new Date()))
                .usadoPor(codigo.estaUsado()
                        ? codigo.getPersonaRegistrada().getNombreCompleto()
                        : null)
                .build();
    }

    private GdPersona personaDe(Long personaId) {
        return personaRepository.findById(personaId)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }

    private GdCasa miCasa(UsuarioAutenticado usuario) {
        return residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(usuario.getPersonaId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa)
                .orElseThrow(() -> GuardianException.solicitudInvalida(MensajesGlobales.SIN_CASA));
    }

    /**
     * Solo el titular invita. Cualquier miembro podria meter gente en la casa
     * de otro, y quien responde por ese hogar es uno solo.
     */
    private GdCasa miCasaComoTitular(UsuarioAutenticado usuario) {
        GdCasa casa = miCasa(usuario);

        Optional<GdResidenteCasa> vinculo = residenteCasaRepository
                .findByPersonaIdAndCasaId(usuario.getPersonaId(), casa.getId());
        boolean esTitular = vinculo
                .map(v -> Codigos.PARENTESCO_TITULAR.equals(v.getParentesco()))
                .orElse(false);

        if (!esTitular) {
            throw GuardianException.sinPermiso(MensajesGlobales.SOLO_TITULAR_FAMILIA);
        }
        return casa;
    }
}
