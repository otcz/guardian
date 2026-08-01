package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarQrRequest;
import guardian.entity.acceso.GdAccesoEvento;
import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdVehiculoRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de la porteria para residentes: inferencia de sentido por presencia,
 * anti-rebote, puertas de denegacion, salida-siempre y frontera de conjunto.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccesoServiceImplTest {

    @Mock private CredencialQrService credencialQrService;
    @Mock private GdCredencialQrRepository credencialRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private GdVehiculoRepository vehiculoRepository;
    @Mock private PresenciaService presenciaService;
    @Mock private InvitacionService invitacionService;
    @Mock private AccesoInvitadoService accesoInvitadoService;
    @Mock private AccesoEventoFabrica fabrica;
    @Mock private GdAccesoEventoRepository eventoRepository;

    @InjectMocks
    private AccesoServiceImpl servicio;

    private UsuarioAutenticado guardia;
    private GdCredencialQr credencial;
    private GdPersona persona;
    private GdCasa casa;

    @BeforeEach
    void preparar() {
        guardia = new UsuarioAutenticado(1L, 10L, 1L, "G1", "Guardia Uno", "GUARDIA");

        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        persona = new GdPersona();
        persona.setId(50L);
        persona.setConjunto(conjunto);
        persona.setNombres("Ana");
        persona.setApellidos("Diaz");
        persona.setDocumento("123");
        persona.setActivo(Codigos.SI);
        persona.setFotoUrl("/api/publico/fotos/x.png");

        credencial = new GdCredencialQr();
        credencial.setId(7L);
        credencial.setPersona(persona);
        credencial.setActivo(Codigos.SI);
        credencial.setUsosRealizados(0);

        casa = new GdCasa();
        casa.setId(3L);
        casa.setConjunto(conjunto);
        casa.setIdentificador("M1-C5");
        casa.setActivo(Codigos.SI);

        GdResidenteCasa vinculo = new GdResidenteCasa();
        vinculo.setPersona(persona);
        vinculo.setCasa(casa);

        lenient().when(credencialQrService.resolver("QR")).thenReturn(Optional.of(credencial));
        lenient().when(residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(50L, Codigos.SI))
                .thenReturn(Optional.of(vinculo));
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
        lenient().when(fabrica.lecturaReciente(anyLong())).thenReturn(Optional.empty());
        lenient().when(vehiculoRepository.findByCasaIdAndActivoOrderByPlacaAsc(anyLong(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
    }

    private VerificarQrRequest verificarRequest() {
        VerificarQrRequest request = new VerificarQrRequest();
        request.setPayload("QR");
        return request;
    }

    private RegistrarAccesoRequest registrarRequest(String sentido, Boolean corregir) {
        RegistrarAccesoRequest request = new RegistrarAccesoRequest();
        request.setPayload("QR");
        request.setModo(Codigos.MODO_PEATON);
        request.setSentido(sentido);
        request.setCorregirSentido(corregir);
        return request;
    }

    // ── Inferencia de sentido ────────────────────────────────────────────────

    @Test
    @DisplayName("afuera -> el sentido sugerido es ENTRADA")
    void sugiereEntradaCuandoEstaAfuera() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isTrue();
        assertThat(ficha.getSentidoSugerido()).isEqualTo(Codigos.ENTRADA);
    }

    @Test
    @DisplayName("adentro -> el sentido sugerido es SALIDA")
    void sugiereSalidaCuandoEstaAdentro() {
        when(presenciaService.estaAdentro(50L)).thenReturn(true);

        FichaVerificacionResponse ficha = servicio.verificar(verificarRequest(), guardia);

        assertThat(ficha.getSentidoSugerido()).isEqualTo(Codigos.SALIDA);
    }

    @Test
    @DisplayName("sentido contradictorio sin flag de correccion -> conflicto")
    void rechazaSentidoContradictorioSinCorreccion() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(registrarRequest(Codigos.SALIDA, null), guardia))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.YA_AFUERA);
    }

    @Test
    @DisplayName("el guardia puede corregir el sentido con el flag explicito")
    void aceptaCorreccionConsciente() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        AccesoEventoResponse evento =
                servicio.registrar(registrarRequest(Codigos.SALIDA, true), guardia);

        assertThat(evento.getSentido()).isEqualTo(Codigos.SALIDA);
        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
    }

    // ── Anti-rebote ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("doble escaneo devuelve el evento existente sin crear otro")
    void dobleEscaneoNoDuplicaEvento() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);
        GdAccesoEvento previo = new GdAccesoEvento();
        previo.setSentido(Codigos.ENTRADA);
        previo.setResultado(Codigos.RESULTADO_PERMITIDO);
        when(fabrica.lecturaReciente(7L)).thenReturn(Optional.of(previo));

        AccesoEventoResponse evento = servicio.registrar(registrarRequest(null, null), guardia);

        assertThat(evento.getSentido()).isEqualTo(Codigos.ENTRADA);
        // Los usos no se incrementan: el evento devuelto es el que ya existia.
        assertThat(credencial.getUsosRealizados()).isZero();
    }

    // ── Puertas de denegacion ────────────────────────────────────────────────

    @Test
    @DisplayName("credencial revocada y persona afuera -> denegado")
    void deniegaCredencialRevocadaAfuera() {
        credencial.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isFalse();
        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_CREDENCIAL_REVOCADA);
        // La identidad se muestra igual: el guardia debe poder explicar el no.
        assertThat(ficha.getNombreCompleto()).isEqualTo("Ana Diaz");
    }

    @Test
    @DisplayName("revocada pero ADENTRO -> la salida se permite siempre")
    void adentroSiemprePuedeSalir() {
        credencial.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(true);

        FichaVerificacionResponse ficha = servicio.verificar(verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isTrue();
        assertThat(ficha.getSentidoSugerido()).isEqualTo(Codigos.SALIDA);
        assertThat(ficha.getMensaje()).isEqualTo(MensajesGlobales.SOLO_SALIDA);

        AccesoEventoResponse evento = servicio.registrar(registrarRequest(null, null), guardia);
        assertThat(evento.getSentido()).isEqualTo(Codigos.SALIDA);
    }

    @Test
    @DisplayName("revocada y adentro: la correccion a ENTRADA se rechaza")
    void soloSalidaNoAdmiteCorreccionAEntrada() {
        credencial.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(registrarRequest(Codigos.ENTRADA, true), guardia))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SOLO_SALIDA);
    }

    @Test
    @DisplayName("persona inactiva -> denegado con su motivo")
    void deniegaPersonaInactiva() {
        persona.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificarRequest(), guardia);

        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_PERSONA_INACTIVA);
    }

    @Test
    @DisplayName("casa inactiva -> denegado con su motivo")
    void deniegaCasaInactiva() {
        casa.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificarRequest(), guardia);

        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_CASA_INACTIVA);
    }

    // ── Frontera de conjunto ─────────────────────────────────────────────────

    @Test
    @DisplayName("credencial de otro conjunto se trata como QR desconocido")
    void credencialDeOtroConjuntoNoSeReconoce() {
        GdConjunto otro = new GdConjunto();
        otro.setId(99L);
        persona.setConjunto(otro);

        FichaVerificacionResponse ficha = servicio.verificar(verificarRequest(), guardia);

        assertThat(ficha.isPermitido()).isFalse();
        assertThat(ficha.getMensaje()).isEqualTo(MensajesGlobales.QR_NO_RECONOCIDO);
        // Sin identidad: para esta porteria ese codigo no existe.
        assertThat(ficha.getNombreCompleto()).isNull();
    }

    // ── Vehiculos ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("vehiculo de otra casa -> rechazado")
    void rechazaVehiculoDeOtraCasa() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        GdCasa otraCasa = new GdCasa();
        otraCasa.setId(77L);
        GdVehiculo ajeno = new GdVehiculo();
        ajeno.setId(5L);
        ajeno.setCasa(otraCasa);
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(ajeno));

        RegistrarAccesoRequest request = registrarRequest(null, null);
        request.setModo(Codigos.MODO_VEHICULO);
        request.setVehiculoId(5L);

        assertThatThrownBy(() -> servicio.registrar(request, guardia))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.VEHICULO_NO_PERTENECE);
    }

    @Test
    @DisplayName("modo VEHICULO sin id de vehiculo -> rechazado")
    void exigeVehiculoEnModoVehiculo() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        RegistrarAccesoRequest request = registrarRequest(null, null);
        request.setModo(Codigos.MODO_VEHICULO);

        assertThatThrownBy(() -> servicio.registrar(request, guardia))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SELECCIONA_VEHICULO);
    }

    // ── Registro exitoso ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registro permitido incrementa los usos de la credencial")
    void registroIncrementaUsos() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        AccesoEventoResponse evento = servicio.registrar(registrarRequest(null, null), guardia);

        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
        assertThat(credencial.getUsosRealizados()).isEqualTo(1);
        verify(credencialRepository).save(credencial);
    }
}
