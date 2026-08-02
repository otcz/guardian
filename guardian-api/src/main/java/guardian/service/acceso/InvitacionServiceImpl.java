package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.invitacion.InvitacionPublicaResponse;
import guardian.dto.invitacion.InvitacionRequest;
import guardian.dto.invitacion.InvitacionResponse;
import guardian.entity.acceso.GdInvitacion;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.exception.GuardianException;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdInvitacionRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.security.Autoridad;
import guardian.security.UsuarioAutenticado;
import guardian.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ciclo de vida de las invitaciones. El QR usa el prefijo GRDI para que la
 * porteria distinga a un invitado de un residente desde el primer byte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitacionServiceImpl implements InvitacionService {

    private static final String PREFIJO = "GRDI";
    private static final String SEPARADOR = ".";
    private static final int PARTES_ESPERADAS = 3;
    private static final int USOS_POR_DEFECTO = 1;

    /** Estados derivados que consume la UI. Nunca se almacenan. */
    private static final String ESTADO_VIGENTE = "VIGENTE";
    private static final String ESTADO_NO_VIGENTE = "NO_VIGENTE";
    private static final String ESTADO_AGOTADA = "AGOTADA";
    private static final String ESTADO_VENCIDA = "VENCIDA";
    private static final String ESTADO_REVOCADA = "REVOCADA";

    /** Margen para "desde ahora": evita rechazar el reloj desincronizado del telefono. */
    private static final long TOLERANCIA_PASADO_MILIS = 15 * 60 * 1_000L;
    private static final long MILIS_POR_DIA = 24 * 60 * 60 * 1_000L;

    private final GdInvitacionRepository invitacionRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdPersonaRepository personaRepository;
    private final GdAccesoEventoRepository eventoRepository;

    @Value("${guardian.security.qr-hmac-secret}")
    private String secretoHmac;

    @Value("${guardian.invitacion.dias-vigencia-maxima}")
    private long diasVigenciaMaxima;

    @Value("${guardian.invitacion.usos-maximos-tope}")
    private int usosMaximosTope;

    // ── Creacion y gestion del anfitrion ─────────────────────────────────────

    @Override
    @Transactional
    public InvitacionResponse crear(InvitacionRequest request, UsuarioAutenticado anfitrion) {
        GdCasa casa = miCasa(anfitrion);
        GdPersona persona = personaRepository.findById(anfitrion.getPersonaId())
                .orElseThrow(() -> GuardianException.noAutorizado(MensajesGlobales.SESION_REQUERIDA));

        Date ahora = new Date();
        Date desde = request.getVigenciaDesde() != null ? request.getVigenciaDesde() : ahora;
        Date hasta = request.getVigenciaHasta() != null
                ? request.getVigenciaHasta()
                : finDelDia(desde);

        if (!hasta.after(desde)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.INVITACION_VIGENCIA_INVALIDA);
        }
        // Una invitacion que nace vencida o en el pasado es siempre un error de
        // digitacion; aceptarla solo confundiria al invitado en la porteria.
        if (hasta.before(ahora)
                || desde.getTime() < ahora.getTime() - TOLERANCIA_PASADO_MILIS) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.INVITACION_EN_PASADO);
        }
        // El QR del invitado es efimero por definicion (CONTEXT.md seccion 3).
        // Sin tope, "efimero" seria una promesa: alguien crearia una invitacion
        // de anos y tendria una credencial permanente sin foto ni control.
        if (hasta.getTime() - desde.getTime() > diasVigenciaMaxima * MILIS_POR_DIA) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.INVITACION_MUY_LARGA);
        }
        if (request.getUsosMaximos() != null && request.getUsosMaximos() > usosMaximosTope) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.INVITACION_MUY_LARGA);
        }

        String codigo = UUID.randomUUID().toString();

        GdInvitacion invitacion = new GdInvitacion();
        invitacion.setConjuntoId(anfitrion.getConjuntoId());
        invitacion.setCasa(casa);
        invitacion.setAnfitrion(persona);
        invitacion.setNombreInvitado(request.getNombreInvitado().trim());
        invitacion.setDocumentoInvitado(request.getDocumentoInvitado().trim());
        invitacion.setPlaca(normalizarPlaca(request.getPlaca()));
        invitacion.setVigenciaDesde(desde);
        invitacion.setVigenciaHasta(hasta);
        invitacion.setUsosMaximos(request.getUsosMaximos() != null
                ? request.getUsosMaximos() : USOS_POR_DEFECTO);
        invitacion.setUsosRealizados(0);
        invitacion.setCodigoPublico(codigo);
        invitacion.setFirmaHash(HmacUtil.firmar(codigo, secretoHmac));
        invitacion.setActivo(Codigos.SI);
        invitacion.setUsuarioCreador(anfitrion.getDocumento());

        GdInvitacion guardada = invitacionRepository.save(invitacion);
        log.info("[invitacion] creada id={} casaId={} anfitrionId={} usos={}",
                guardada.getId(), casa.getId(), persona.getId(), guardada.getUsosMaximos());

        return mapear(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitacionResponse> listarDeMiCasa(UsuarioAutenticado usuario) {
        return invitacionRepository.findByCasaIdOrderByIdDesc(miCasa(usuario).getId())
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvitacionResponse revocar(Long id, UsuarioAutenticado usuario) {
        GdInvitacion invitacion = invitacionRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        // Cualquier residente de la casa puede revocar, no solo quien invito:
        // si el anfitrion no esta, la casa igual tiene que poder cerrar la puerta.
        if (!invitacion.getCasa().getId().equals(miCasa(usuario).getId())) {
            throw GuardianException.sinPermiso(MensajesGlobales.FAMILIAR_AJENO);
        }
        return revocarInterno(invitacion, usuario.getDocumento());
    }

    // ── Administrador ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InvitacionResponse> listarDelConjunto(Long conjuntoId) {
        return invitacionRepository.findByConjuntoIdOrderByIdDesc(conjuntoId)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvitacionResponse revocarComoAdmin(Long id, UsuarioAutenticado admin) {
        GdInvitacion invitacion = invitacionRepository.findById(id)
                .filter(i -> i.getConjuntoId().equals(admin.getConjuntoId()))
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        return revocarInterno(invitacion, admin.getDocumento());
    }

    @Override
    @Transactional
    public void eliminarComoSuperAdmin(Long id, UsuarioAutenticado ejecutor) {
        Autoridad.exigirSuperAdmin(ejecutor);

        GdInvitacion invitacion = invitacionRepository.findById(id)
                .filter(i -> i.getConjuntoId().equals(ejecutor.getConjuntoId()))
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        eliminarInterno(invitacion, ejecutor.getDocumento());
    }

    // ── Invitado (publico) y porteria ────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InvitacionPublicaResponse publica(String codigoPublico) {
        GdInvitacion invitacion = invitacionRepository.buscarPorCodigoPublico(codigoPublico)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        String estado = estadoDe(invitacion);

        // El payload solo viaja mientras el QR sirva de algo. Una invitacion
        // revocada, vencida o agotada no debe seguir repartiendo un codigo
        // escaneable — ni el nombre del anfitrion — por un link viejo.
        boolean util = ESTADO_VIGENTE.equals(estado) || ESTADO_NO_VIGENTE.equals(estado);

        return InvitacionPublicaResponse.builder()
                .nombreInvitado(invitacion.getNombreInvitado())
                .casaIdentificador(invitacion.getCasa().getIdentificador())
                .anfitrionNombre(util ? invitacion.getAnfitrion().getNombreCompleto() : null)
                .vigenciaDesde(invitacion.getVigenciaDesde())
                .vigenciaHasta(invitacion.getVigenciaHasta())
                .usosRestantes(Math.max(0,
                        invitacion.getUsosMaximos() - invitacion.getUsosRealizados()))
                .estado(estado)
                .payload(util ? construirPayload(invitacion) : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GdInvitacion> resolver(String payload) {
        if (payload == null) {
            return Optional.empty();
        }

        String[] partes = payload.trim().split("\\" + SEPARADOR);
        if (partes.length != PARTES_ESPERADAS || !PREFIJO.equals(partes[0])) {
            return Optional.empty();
        }

        // La firma se recalcula ANTES de tocar la base, igual que con las
        // credenciales: un codigo inventado no merece ni un query.
        String firmaEsperada = HmacUtil.firmar(partes[1], secretoHmac);
        if (!HmacUtil.coincide(firmaEsperada, partes[2])) {
            log.info("[invitacion] firma invalida codigo={}", partes[1]);
            return Optional.empty();
        }

        return invitacionRepository.buscarPorCodigoPublico(partes[1]);
    }

    @Override
    public String construirPayload(GdInvitacion invitacion) {
        return PREFIJO + SEPARADOR + invitacion.getCodigoPublico()
                + SEPARADOR + invitacion.getFirmaHash();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private InvitacionResponse revocarInterno(GdInvitacion invitacion, String ejecutor) {
        invitacion.setActivo(Codigos.NO);
        invitacion.setUsuarioModificador(ejecutor);
        GdInvitacion guardada = invitacionRepository.save(invitacion);

        log.info("[invitacion] revocada id={} por={}", invitacion.getId(), ejecutor);
        return mapear(guardada);
    }

    private void eliminarInterno(GdInvitacion invitacion, String ejecutor) {
        Long id = invitacion.getId();

        // La bitacora primero: el evento apunta a la invitacion por FK y el
        // borrado reventaria contra la restriccion. Los eventos se quedan —
        // llevan el nombre y el documento del invitado copiados encima — y solo
        // pierden el enlace a una fila que ya no existe.
        eventoRepository.desvincularInvitacion(id);
        invitacionRepository.delete(invitacion);

        log.warn("[invitacion] ELIMINADA id={} invitado={} por={}",
                id, invitacion.getNombreInvitado(), ejecutor);
    }

    private GdCasa miCasa(UsuarioAutenticado usuario) {
        return residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(usuario.getPersonaId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa)
                .orElseThrow(() -> GuardianException.solicitudInvalida(MensajesGlobales.SIN_CASA));
    }

    /** Default de vigencia: la visita es "hoy", hasta la medianoche. */
    private Date finDelDia(Date referencia) {
        LocalDateTime fin = LocalDateTime.ofInstant(referencia.toInstant(), ZoneId.systemDefault())
                .toLocalDate()
                .atTime(LocalTime.MAX);
        return Date.from(fin.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String normalizarPlaca(String placa) {
        if (placa == null) {
            return null;
        }
        String normalizada = placa.trim().toUpperCase().replaceAll("[\\s-]", "");
        // "- -" o solo espacios quedarian como cadena vacia: eso es "a pie",
        // no una placa. Persistirla rompe la regla de la garita de que placa
        // presente = puede entrar en vehiculo.
        return normalizada.isEmpty() ? null : normalizada;
    }

    private String estadoDe(GdInvitacion invitacion) {
        Date ahora = new Date();
        if (!invitacion.estaActivo()) {
            return ESTADO_REVOCADA;
        }
        if (invitacion.vencida(ahora)) {
            return ESTADO_VENCIDA;
        }
        if (invitacion.agotada()) {
            return ESTADO_AGOTADA;
        }
        if (invitacion.aunNoVigente(ahora)) {
            return ESTADO_NO_VIGENTE;
        }
        return ESTADO_VIGENTE;
    }

    private InvitacionResponse mapear(GdInvitacion invitacion) {
        return InvitacionResponse.builder()
                .id(invitacion.getId())
                .nombreInvitado(invitacion.getNombreInvitado())
                .documentoInvitado(invitacion.getDocumentoInvitado())
                .placa(invitacion.getPlaca())
                .vigenciaDesde(invitacion.getVigenciaDesde())
                .vigenciaHasta(invitacion.getVigenciaHasta())
                .usosMaximos(invitacion.getUsosMaximos())
                .usosRealizados(invitacion.getUsosRealizados())
                .estado(estadoDe(invitacion))
                .casaIdentificador(invitacion.getCasa().getIdentificador())
                .anfitrionNombre(invitacion.getAnfitrion().getNombreCompleto())
                .codigoPublico(invitacion.getCodigoPublico())
                .payload(construirPayload(invitacion))
                .build();
    }
}
