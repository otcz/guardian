package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.entity.acceso.GdAccesoEvento;
import guardian.entity.conjunto.GdPuntoAcceso;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdPuntoAccesoRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Construccion, anti-rebote y mapeo de eventos de acceso.
 *
 * <p>Es el terreno comun de los dos flujos de la porteria — residentes e
 * invitados — extraido para que ninguno pueda divergir del otro en como escribe
 * la bitacora.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccesoEventoFabrica {

    private static final long MILIS_POR_SEGUNDO = 1_000L;

    private final GdAccesoEventoRepository eventoRepository;
    private final GdPersonaRepository personaRepository;
    private final GdPuntoAccesoRepository puntoAccesoRepository;

    @Value("${guardian.acceso.segundos-anti-rebote}")
    private long segundosAntiRebote;

    /**
     * Evento base con conjunto, guardia y punto de acceso resueltos.
     *
     * <p>Si la peticion no trae punto y el conjunto tiene exactamente uno
     * activo, se usa ese: la tablet de una porteria unica no deberia tener que
     * configurarlo, y sin este default la columna quedaria siempre nula.</p>
     */
    public GdAccesoEvento nuevoEvento(UsuarioAutenticado guardia, Long puntoAccesoId) {
        // Falla cerrado y ANTES de construir nada. Lo grave de no hacerlo no
        // seria el 500: las rutas de DENEGACION pasan por esta misma fabrica,
        // asi que el intento fallido tampoco quedaria registrado y se perderia
        // la evidencia que el negocio declara no negociable.
        if (guardia.getConjuntoId() == null) {
            throw guardian.exception.GuardianException.sinPermiso(
                    guardian.constant.MensajesGlobales.SIN_SEDE_ELEGIDA);
        }

        GdAccesoEvento evento = new GdAccesoEvento();
        evento.setConjuntoId(guardia.getConjuntoId());
        evento.setFechaEvento(new Date());
        evento.setActivo(Codigos.SI);
        evento.setUsuarioCreador(guardia.getDocumento());
        evento.setGuardiaNombre(guardia.getNombreCompleto());

        personaRepository.findById(guardia.getPersonaId()).ifPresent(evento::setGuardia);

        evento.setPuntoAcceso(resolverPunto(guardia, puntoAccesoId));
        return evento;
    }

    /**
     * Por donde paso la persona.
     *
     * <p>El id que llega es de la TABLET, no del token: hay que tratarlo como
     * entrada no confiable. Se resuelve contra el conjunto del guardia y no con
     * un findById suelto — sin ese filtro, una tablet con la porteria de otra
     * sede guardada estampa un punto de acceso ajeno sobre un evento de esta, y
     * la bitacora no queda incompleta sino MENTIROSA, que es peor.</p>
     *
     * <p>Cuando no se puede resolver se cae al unico activo, si es que hay uno
     * solo, y se deja constancia en el log. Antes un id invalido no hacia nada
     * y la columna quedaba nula en silencio: el problema aparecia meses despues,
     * leyendo una bitacora que ya no podia responder por donde entro nadie.</p>
     */
    private GdPuntoAcceso resolverPunto(UsuarioAutenticado guardia, Long puntoAccesoId) {
        Long conjuntoId = guardia.getConjuntoId();

        if (puntoAccesoId != null) {
            Optional<GdPuntoAcceso> pedido =
                    puntoAccesoRepository.findByIdAndConjuntoId(puntoAccesoId, conjuntoId);
            if (pedido.isPresent()) {
                return pedido.get();
            }
            log.warn("[acceso] la tablet mando puntoAccesoId={} que no es de la sede {}"
                    + " (guardia={}); se cae al punto por defecto",
                    puntoAccesoId, conjuntoId, guardia.getDocumento());
        }

        List<GdPuntoAcceso> activos = puntoAccesoRepository
                .findByConjuntoIdAndActivoOrderByNombreAsc(conjuntoId, Codigos.SI);
        if (activos.size() == 1) {
            return activos.get(0);
        }

        // Con dos o mas porterias activas no hay forma de adivinar, y adivinar
        // seria lo peor que se puede hacer aca. Queda nulo, pero NO en silencio.
        log.warn("[acceso] evento sin punto de acceso: la sede {} tiene {} porterias"
                + " activas y la tablet del guardia {} no dijo cual",
                conjuntoId, activos.size(), guardia.getDocumento());
        return null;
    }

    public GdAccesoEvento guardar(GdAccesoEvento evento) {
        return eventoRepository.save(evento);
    }

    /**
     * Ultimo evento PERMITIDO de la credencial dentro de la ventana
     * anti-rebote. El guardia que escanea dos veces por nervios no debe
     * generar una entrada y una salida fantasma.
     */
    public Optional<GdAccesoEvento> lecturaReciente(Long credencialId) {
        return eventoRepository
                .findByCredencialIdAndFechaEventoAfterOrderByFechaEventoDesc(
                        credencialId, inicioVentana())
                .stream()
                .filter(GdAccesoEvento::fuePermitido)
                .findFirst();
    }

    /** Espejo del anti-rebote para invitaciones (evento sin persona). */
    public Optional<GdAccesoEvento> lecturaRecienteInvitacion(Long invitacionId) {
        return eventoRepository
                .findByInvitacionIdAndFechaEventoAfterOrderByFechaEventoDesc(
                        invitacionId, inicioVentana())
                .stream()
                .filter(GdAccesoEvento::fuePermitido)
                .findFirst();
    }

    public AccesoEventoResponse mapear(GdAccesoEvento evento) {
        return AccesoEventoResponse.builder()
                .id(evento.getId())
                .fechaEvento(evento.getFechaEvento())
                .sentido(evento.getSentido())
                .modo(evento.getModo())
                .resultado(evento.getResultado())
                .motivoDenegacion(evento.getMotivoDenegacion())
                .personaNombre(evento.getPersonaNombre())
                .personaDocumento(evento.getPersonaDocumento())
                .casaIdentificador(evento.getCasaIdentificador())
                .vehiculoPlaca(evento.getVehiculoPlaca())
                .guardiaNombre(evento.getGuardiaNombre())
                .puntoAcceso(evento.getPuntoAcceso() != null
                        ? evento.getPuntoAcceso().getNombre()
                        : null)
                .invitado(evento.getInvitacion() != null)
                .build();
    }

    private Date inicioVentana() {
        return new Date(System.currentTimeMillis() - segundosAntiRebote * MILIS_POR_SEGUNDO);
    }
}
