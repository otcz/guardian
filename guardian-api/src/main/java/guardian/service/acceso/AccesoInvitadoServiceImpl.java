package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarQrRequest;
import guardian.entity.acceso.GdAccesoEvento;
import guardian.entity.acceso.GdInvitacion;
import guardian.exception.GuardianException;
import guardian.repository.GdInvitacionRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccesoInvitadoServiceImpl implements AccesoInvitadoService {

    private final GdInvitacionRepository invitacionRepository;
    private final PresenciaService presenciaService;
    private final AccesoEventoFabrica fabrica;

    @Override
    @Transactional
    public FichaVerificacionResponse verificar(GdInvitacion invitacion,
                                               VerificarQrRequest request,
                                               UsuarioAutenticado guardia) {
        if (deOtroConjunto(invitacion, guardia)) {
            return fichaDesconocido(request.getPayload(), guardia, request.getPuntoAccesoId());
        }

        String motivo = motivoDenegacion(invitacion);
        if (motivo != null) {
            registrarDenegacion(invitacion, motivo, guardia, request.getPuntoAccesoId());
            return ficha(invitacion, false, motivo, request.getPayload());
        }
        return ficha(invitacion, true, null, request.getPayload());
    }

    @Override
    @Transactional
    public AccesoEventoResponse registrar(GdInvitacion invitacion,
                                          RegistrarAccesoRequest request,
                                          UsuarioAutenticado guardia) {
        if (deOtroConjunto(invitacion, guardia)) {
            // Para esta porteria el codigo no existe, pero el intento si queda:
            // sin invitacion enlazada, para no colgar de la bitacora de esta
            // sede una fila que pertenece a otra.
            return fabrica.mapear(registrarDenegacionAnonima(guardia, request));
        }

        // Se revalida igual que con credenciales: entre el escaneo y el toque
        // del guardia el anfitrion pudo revocar la invitacion.
        String motivo = motivoDenegacion(invitacion);
        if (motivo != null) {
            return fabrica.mapear(registrarDenegacion(
                    invitacion, motivo, guardia, request));
        }

        Optional<GdAccesoEvento> reciente = fabrica.lecturaRecienteInvitacion(invitacion.getId());
        if (reciente.isPresent()) {
            log.info("[acceso] doble escaneo de invitacion ignorado invitacionId={} eventoId={}",
                    invitacion.getId(), reciente.get().getId());
            return fabrica.mapear(reciente.get());
        }

        boolean adentro = presenciaService.estaAdentroInvitado(invitacion.getId());
        String sentido = resolverSentido(request, adentro, invitacion.getId());

        // El invitado no tiene vehiculos registrados en el conjunto: su unica
        // placa valida es la declarada en la invitacion. Que llegue en carro
        // sin haberla declarado es un intento negado, no un error de pantalla:
        // va a la bitacora.
        String modo = request.getModo();
        if (Codigos.MODO_VEHICULO.equals(modo) && invitacion.getPlaca() == null) {
            return fabrica.mapear(registrarDenegacion(invitacion,
                    Codigos.MOTIVO_INVITADO_SIN_VEHICULO, guardia, request));
        }

        GdAccesoEvento evento = fabrica.nuevoEvento(guardia, request.getPuntoAccesoId());
        evento.setSentido(sentido);
        evento.setModo(modo);
        evento.setResultado(Codigos.RESULTADO_PERMITIDO);
        evento.setInvitacion(invitacion);
        evento.setPersonaNombre(invitacion.getNombreInvitado());
        evento.setPersonaDocumento(invitacion.getDocumentoInvitado());
        evento.setCasa(invitacion.getCasa());
        evento.setCasaIdentificador(invitacion.getCasa().getIdentificador());
        if (Codigos.MODO_VEHICULO.equals(modo)) {
            evento.setVehiculoPlaca(invitacion.getPlaca());
        }

        GdAccesoEvento guardado = fabrica.guardar(evento);

        // Solo la ENTRADA consume uso: nadie debe quedar atrapado adentro
        // porque su invitacion "se gasto".
        if (Codigos.ENTRADA.equals(sentido)) {
            invitacion.setUsosRealizados(invitacion.getUsosRealizados() + 1);
            invitacionRepository.save(invitacion);
        }

        log.info("[acceso] invitado registrado invitacionId={} sentido={} usos={}/{} guardiaId={}",
                invitacion.getId(), sentido, invitacion.getUsosRealizados(),
                invitacion.getUsosMaximos(), guardia.getPersonaId());

        return fabrica.mapear(guardado);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reglas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Puertas de la invitacion. La PRIMERA pregunta es si el invitado ya esta
     * adentro: quien esta adentro siempre puede salir — aunque la invitacion
     * se haya revocado, vencido o agotado, o la casa se haya inhabilitado
     * mientras tanto. Retener a alguien dentro del conjunto no protege a
     * nadie; lo unico que se le cierra es la proxima ENTRADA.
     */
    private String motivoDenegacion(GdInvitacion invitacion) {
        if (presenciaService.estaAdentroInvitado(invitacion.getId())) {
            return null;
        }

        Date ahora = new Date();
        if (!invitacion.puedeOperar()) {
            return Codigos.MOTIVO_CREDENCIAL_REVOCADA;
        }
        if (!invitacion.estaAprobada()) {
            return Codigos.MOTIVO_INVITACION_PENDIENTE_APROBACION;
        }
        if (invitacion.getCasa().estaBloqueado()) {
            return Codigos.MOTIVO_CASA_BLOQUEADA;
        }
        if (!invitacion.getCasa().estaActivo()) {
            return Codigos.MOTIVO_CASA_INACTIVA;
        }
        if (invitacion.aunNoVigente(ahora)) {
            return Codigos.MOTIVO_INVITACION_NO_VIGENTE;
        }
        if (invitacion.vencida(ahora)) {
            return Codigos.MOTIVO_CREDENCIAL_VENCIDA;
        }
        if (invitacion.agotada()) {
            return Codigos.MOTIVO_INVITACION_AGOTADA;
        }
        return null;
    }

    /**
     * El sentido lo decide la presencia. Un sentido explicito que la
     * contradiga solo se acepta con {@code corregirSentido}: es la correccion
     * consciente del guardia (CONTEXT.md seccion 4), no una pantalla vieja.
     */
    private String resolverSentido(RegistrarAccesoRequest request, boolean adentro,
                                   Long invitacionId) {
        String porPresencia = adentro ? Codigos.SALIDA : Codigos.ENTRADA;

        if (request.getSentido() == null || request.getSentido().equals(porPresencia)) {
            return porPresencia;
        }
        if (Boolean.TRUE.equals(request.getCorregirSentido())) {
            log.warn("[acceso] sentido corregido por el guardia invitacionId={} de={} a={}",
                    invitacionId, porPresencia, request.getSentido());
            return request.getSentido();
        }
        throw GuardianException.conflicto(Codigos.ENTRADA.equals(request.getSentido())
                ? MensajesGlobales.YA_ADENTRO
                : MensajesGlobales.YA_AFUERA);
    }

    private boolean deOtroConjunto(GdInvitacion invitacion, UsuarioAutenticado guardia) {
        boolean ajena = !invitacion.getConjuntoId().equals(guardia.getConjuntoId());
        if (ajena) {
            // Frontera de tenant: para esta porteria el codigo simplemente no
            // existe. Ni la ficha ni el log revelan de que conjunto es.
            log.warn("[acceso] invitacion de otro conjunto invitacionId={} guardiaConjunto={}",
                    invitacion.getId(), guardia.getConjuntoId());
        }
        return ajena;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escritura y fichas
    // ─────────────────────────────────────────────────────────────────────────

    /** Denegacion desde la VERIFICACION, donde no hay peticion de registro. */
    private GdAccesoEvento registrarDenegacion(GdInvitacion invitacion, String motivo,
                                               UsuarioAutenticado guardia, Long puntoAccesoId) {
        return escribirDenegacion(invitacion, motivo, guardia, puntoAccesoId,
                Codigos.MODO_PEATON, null);
    }

    /**
     * Denegacion desde el REGISTRO. Conserva el modo que el guardia toco y la
     * placa declarada: leer despues "se nego a pie" cuando el intento fue en
     * carro convierte la bitacora en un relato distinto del que ocurrio.
     */
    private GdAccesoEvento registrarDenegacion(GdInvitacion invitacion, String motivo,
                                               UsuarioAutenticado guardia,
                                               RegistrarAccesoRequest request) {
        String modo = request.getModo() != null ? request.getModo() : Codigos.MODO_PEATON;
        String placa = Codigos.MODO_VEHICULO.equals(modo) ? invitacion.getPlaca() : null;
        return escribirDenegacion(invitacion, motivo, guardia, request.getPuntoAccesoId(),
                modo, placa);
    }

    /**
     * Intento que no se pudo atribuir a nadie de esta sede. Queda la fila con
     * el guardia, la hora y el punto: es lo unico que se sabe, y es mas de lo
     * que quedaba antes, que era nada.
     */
    private GdAccesoEvento registrarDenegacionAnonima(UsuarioAutenticado guardia,
                                                      RegistrarAccesoRequest request) {
        GdAccesoEvento evento = fabrica.nuevoEvento(guardia, request.getPuntoAccesoId());
        evento.setSentido(Codigos.ENTRADA);
        evento.setModo(request.getModo() != null ? request.getModo() : Codigos.MODO_PEATON);
        evento.setResultado(Codigos.RESULTADO_DENEGADO);
        evento.setMotivoDenegacion(Codigos.MOTIVO_FIRMA_INVALIDA);

        log.info("[acceso] invitado de otra sede denegado guardiaId={}", guardia.getPersonaId());
        return fabrica.guardar(evento);
    }

    private GdAccesoEvento escribirDenegacion(GdInvitacion invitacion, String motivo,
                                              UsuarioAutenticado guardia, Long puntoAccesoId,
                                              String modo, String placa) {
        GdAccesoEvento evento = fabrica.nuevoEvento(guardia, puntoAccesoId);
        evento.setSentido(Codigos.ENTRADA);
        evento.setModo(modo);
        evento.setResultado(Codigos.RESULTADO_DENEGADO);
        evento.setMotivoDenegacion(motivo);
        evento.setInvitacion(invitacion);
        evento.setPersonaNombre(invitacion.getNombreInvitado());
        evento.setPersonaDocumento(invitacion.getDocumentoInvitado());
        evento.setCasa(invitacion.getCasa());
        evento.setCasaIdentificador(invitacion.getCasa().getIdentificador());
        evento.setVehiculoPlaca(placa);

        log.info("[acceso] invitado denegado motivo={} invitacionId={} guardiaId={}",
                motivo, invitacion.getId(), guardia.getPersonaId());

        return fabrica.guardar(evento);
    }

    /**
     * Ficha del invitado. Sin foto — el control es el documento fisico contra
     * el declarado — y con el anfitrion visible para que el guardia pueda
     * confirmar con la casa si algo no cuadra.
     */
    private FichaVerificacionResponse ficha(GdInvitacion invitacion, boolean permitido,
                                            String motivo, String payload) {
        return FichaVerificacionResponse.builder()
                .permitido(permitido)
                .motivoDenegacion(motivo)
                .mensaje(permitido ? null : mensajeDe(motivo))
                .nombreCompleto(invitacion.getNombreInvitado())
                .documento(invitacion.getDocumentoInvitado())
                .casaIdentificador(invitacion.getCasa().getIdentificador())
                .esInvitado(true)
                .anfitrionNombre(invitacion.getAnfitrion().getNombreCompleto())
                .invitacionPlaca(invitacion.getPlaca())
                .sentidoSugerido(permitido ? sentidoPorPresencia(invitacion.getId()) : null)
                .vehiculos(Collections.emptyList())
                .payload(payload)
                .build();
    }

    /** Un codigo de otro conjunto se trata igual que un codigo desconocido. */
    private FichaVerificacionResponse fichaDesconocido(String payload,
                                                       UsuarioAutenticado guardia,
                                                       Long puntoAccesoId) {
        GdAccesoEvento evento = fabrica.nuevoEvento(guardia, puntoAccesoId);
        evento.setSentido(Codigos.ENTRADA);
        evento.setModo(Codigos.MODO_PEATON);
        evento.setResultado(Codigos.RESULTADO_DENEGADO);
        evento.setMotivoDenegacion(Codigos.MOTIVO_FIRMA_INVALIDA);
        fabrica.guardar(evento);

        return FichaVerificacionResponse.builder()
                .permitido(false)
                .motivoDenegacion(Codigos.MOTIVO_FIRMA_INVALIDA)
                .mensaje(MensajesGlobales.QR_NO_RECONOCIDO)
                .vehiculos(Collections.emptyList())
                .payload(payload)
                .build();
    }

    private String sentidoPorPresencia(Long invitacionId) {
        return presenciaService.estaAdentroInvitado(invitacionId)
                ? Codigos.SALIDA
                : Codigos.ENTRADA;
    }

    private String mensajeDe(String motivo) {
        switch (motivo) {
            case Codigos.MOTIVO_INVITACION_PENDIENTE_APROBACION:
                return MensajesGlobales.INVITACION_PENDIENTE_APROBACION_MSG;
            case Codigos.MOTIVO_INVITACION_NO_VIGENTE:
                return MensajesGlobales.INVITACION_NO_VIGENTE_MSG;
            case Codigos.MOTIVO_INVITACION_AGOTADA:
                return MensajesGlobales.INVITACION_AGOTADA_MSG;
            case Codigos.MOTIVO_CREDENCIAL_REVOCADA:
                return MensajesGlobales.QR_REVOCADO;
            case Codigos.MOTIVO_CREDENCIAL_VENCIDA:
                return MensajesGlobales.QR_VENCIDO;
            case Codigos.MOTIVO_CASA_INACTIVA:
                return MensajesGlobales.CASA_INACTIVA;
            default:
                return MensajesGlobales.QR_NO_RECONOCIDO;
        }
    }
}
