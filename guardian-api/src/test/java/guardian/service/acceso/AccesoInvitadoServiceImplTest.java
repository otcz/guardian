package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarQrRequest;
import guardian.entity.acceso.GdAccesoEvento;
import guardian.entity.acceso.GdInvitacion;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.exception.GuardianException;
import guardian.repository.GdInvitacionRepository;
import guardian.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Puertas del invitado: orden de gravedad, salida-siempre, consumo de usos
 * solo en la ENTRADA y placa unica declarada en la invitacion.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccesoInvitadoServiceImplTest {

    private static final long HORA_MILIS = 3_600_000L;

    @Mock private GdInvitacionRepository invitacionRepository;
    @Mock private PresenciaService presenciaService;
    @Mock private AccesoEventoFabrica fabrica;

    @InjectMocks
    private AccesoInvitadoServiceImpl servicio;

    private UsuarioAutenticado guardia;
    private GdInvitacion invitacion;

    @BeforeEach
    void preparar() {
        guardia = new UsuarioAutenticado(1L, 10L, 1L, "G1", "Guardia Uno", "GUARDIA");

        GdCasa casa = new GdCasa();
        casa.setId(3L);
        casa.setIdentificador("M1-C5");
        casa.setActivo(Codigos.SI);

        GdPersona anfitrion = new GdPersona();
        anfitrion.setNombres("Ana");
        anfitrion.setApellidos("Diaz");

        invitacion = new GdInvitacion();
        invitacion.setId(20L);
        invitacion.setConjuntoId(1L);
        invitacion.setCasa(casa);
        invitacion.setAnfitrion(anfitrion);
        invitacion.setNombreInvitado("Pedro Perez");
        invitacion.setDocumentoInvitado("999");
        invitacion.setActivo(Codigos.SI);
        invitacion.setBloqueado(Codigos.NO);
        invitacion.setEstadoAprobacion(Codigos.SOLICITUD_APROBADA);
        casa.setBloqueado(Codigos.NO);
        invitacion.setVigenciaDesde(new Date(System.currentTimeMillis() - HORA_MILIS));
        invitacion.setVigenciaHasta(new Date(System.currentTimeMillis() + HORA_MILIS));
        invitacion.setUsosMaximos(1);
        invitacion.setUsosRealizados(0);

        lenient().when(fabrica.nuevoEvento(any(), any()))
                .thenAnswer(inv -> new GdAccesoEvento());
        lenient().when(fabrica.guardar(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(fabrica.mapear(any())).thenAnswer(inv -> {
            GdAccesoEvento e = inv.getArgument(0);
            return AccesoEventoResponse.builder()
                    .sentido(e.getSentido())
                    .resultado(e.getResultado())
                    .motivoDenegacion(e.getMotivoDenegacion())
                    .build();
        });
        lenient().when(fabrica.lecturaRecienteInvitacion(anyLong()))
                .thenReturn(Optional.empty());
    }

    private VerificarQrRequest verificarRequest() {
        VerificarQrRequest request = new VerificarQrRequest();
        request.setPayload("QRI");
        return request;
    }

    private RegistrarAccesoRequest registrarRequest(String modo) {
        RegistrarAccesoRequest request = new RegistrarAccesoRequest();
        request.setPayload("QRI");
        request.setModo(modo);
        return request;
    }

    @Test
    @DisplayName("invitacion vigente y afuera -> puede entrar")
    void permiteEntradaVigente() {
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(invitacion, verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isTrue();
        assertThat(ficha.isEsInvitado()).isTrue();
        assertThat(ficha.getSentidoSugerido()).isEqualTo(Codigos.ENTRADA);
        assertThat(ficha.getAnfitrionNombre()).isEqualTo("Ana Diaz");
    }

    @Test
    @DisplayName("la ENTRADA consume uso; la invitacion de 1 uso queda agotada")
    void entradaConsumeUso() {
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(false);

        AccesoEventoResponse evento =
                servicio.registrar(invitacion, registrarRequest(Codigos.MODO_PEATON), guardia);

        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
        assertThat(invitacion.getUsosRealizados()).isEqualTo(1);
        verify(invitacionRepository).save(invitacion);
    }

    @Test
    @DisplayName("la SALIDA nunca consume uso")
    void salidaNoConsumeUso() {
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(true);

        AccesoEventoResponse evento =
                servicio.registrar(invitacion, registrarRequest(Codigos.MODO_PEATON), guardia);

        assertThat(evento.getSentido()).isEqualTo(Codigos.SALIDA);
        assertThat(invitacion.getUsosRealizados()).isZero();
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("agotada y afuera -> denegada con su motivo")
    void deniegaAgotada() {
        invitacion.setUsosRealizados(1);
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(invitacion, verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isFalse();
        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_INVITACION_AGOTADA);
    }

    @Test
    @DisplayName("pendiente de aprobacion y afuera -> denegada, aunque este vigente")
    void deniegaPendienteDeAprobacion() {
        invitacion.setEstadoAprobacion(Codigos.SOLICITUD_PENDIENTE);
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(invitacion, verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isFalse();
        assertThat(ficha.getMotivoDenegacion())
                .isEqualTo(Codigos.MOTIVO_INVITACION_PENDIENTE_APROBACION);
    }

    @Test
    @DisplayName("pendiente de aprobacion pero ADENTRO -> la salida se permite siempre")
    void adentroSiemprePuedeSalirAunquePendiente() {
        invitacion.setEstadoAprobacion(Codigos.SOLICITUD_PENDIENTE);
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(true);

        AccesoEventoResponse evento =
                servicio.registrar(invitacion, registrarRequest(Codigos.MODO_PEATON), guardia);

        assertThat(evento.getSentido()).isEqualTo(Codigos.SALIDA);
        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
    }

    @Test
    @DisplayName("revocada y afuera -> denegada")
    void deniegaRevocada() {
        invitacion.setActivo(Codigos.NO);
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(invitacion, verificarRequest(), guardia);

        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_CREDENCIAL_REVOCADA);
    }

    @Test
    @DisplayName("revocada pero ADENTRO -> la salida se permite siempre")
    void adentroSiemprePuedeSalirAunqueRevocada() {
        invitacion.setActivo(Codigos.NO);
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(true);

        FichaVerificacionResponse ficha = servicio.verificar(invitacion, verificarRequest(), guardia);
        assertThat(ficha.isPermitido()).isTrue();
        assertThat(ficha.getSentidoSugerido()).isEqualTo(Codigos.SALIDA);

        AccesoEventoResponse evento =
                servicio.registrar(invitacion, registrarRequest(Codigos.MODO_PEATON), guardia);
        assertThat(evento.getSentido()).isEqualTo(Codigos.SALIDA);
        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
    }

    @Test
    @DisplayName("vencida pero ADENTRO -> puede salir (nadie queda atrapado)")
    void vencidaAdentroPuedeSalir() {
        invitacion.setVigenciaHasta(new Date(System.currentTimeMillis() - HORA_MILIS));
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(true);

        AccesoEventoResponse evento =
                servicio.registrar(invitacion, registrarRequest(Codigos.MODO_PEATON), guardia);

        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
        assertThat(evento.getSentido()).isEqualTo(Codigos.SALIDA);
    }

    @Test
    @DisplayName("modo VEHICULO sin placa declarada -> denegado y registrado")
    void rechazaVehiculoSinPlacaDeclarada() {
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(false);

        AccesoEventoResponse evento =
                servicio.registrar(invitacion, registrarRequest(Codigos.MODO_VEHICULO), guardia);

        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_DENEGADO);
        assertThat(evento.getMotivoDenegacion())
                .isEqualTo(Codigos.MOTIVO_INVITADO_SIN_VEHICULO);
    }

    @Test
    @DisplayName("invitacion de otro conjunto no se reconoce en esta porteria")
    void invitacionDeOtroConjuntoNoSeReconoce() {
        invitacion.setConjuntoId(99L);

        FichaVerificacionResponse ficha = servicio.verificar(invitacion, verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isFalse();
        assertThat(ficha.getMensaje()).isEqualTo(MensajesGlobales.QR_NO_RECONOCIDO);
        assertThat(ficha.getNombreCompleto()).isNull();
    }

    @Test
    @DisplayName("doble escaneo devuelve el evento existente sin consumir otro uso")
    void dobleEscaneoNoConsumeOtroUso() {
        when(presenciaService.estaAdentroInvitado(20L)).thenReturn(false);
        GdAccesoEvento previo = new GdAccesoEvento();
        previo.setSentido(Codigos.ENTRADA);
        previo.setResultado(Codigos.RESULTADO_PERMITIDO);
        when(fabrica.lecturaRecienteInvitacion(20L)).thenReturn(Optional.of(previo));

        servicio.registrar(invitacion, registrarRequest(Codigos.MODO_PEATON), guardia);

        assertThat(invitacion.getUsosRealizados()).isZero();
        verify(invitacionRepository, never()).save(any());
    }
}
