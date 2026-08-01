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

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Las DOS LLAVES en la porteria: activo lo mueve el residente, bloqueado solo
 * la administracion, y bloqueado gana siempre.
 *
 * <p>Los fixtures fijan 'bloqueado' EXPLICITAMENTE porque en un test unitario
 * @PrePersist nunca corre: sin eso el campo queda null, Codigos.SI.equals(null)
 * da false, y la prueba pasaria por accidente sin comprobar nada.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccesoBloqueoTest {

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
    private GdResidenteCasa vinculo;
    private GdVehiculo vehiculo;

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
        persona.setDocumento("1001");
        persona.setActivo(Codigos.SI);
        persona.setBloqueado(Codigos.NO);

        credencial = new GdCredencialQr();
        credencial.setId(7L);
        credencial.setPersona(persona);
        credencial.setActivo(Codigos.SI);
        credencial.setBloqueado(Codigos.NO);
        credencial.setUsosRealizados(0);

        casa = new GdCasa();
        casa.setId(3L);
        casa.setConjunto(conjunto);
        casa.setIdentificador("M1-C5");
        casa.setActivo(Codigos.SI);
        casa.setBloqueado(Codigos.NO);

        vinculo = new GdResidenteCasa();
        vinculo.setPersona(persona);
        vinculo.setCasa(casa);
        vinculo.setActivo(Codigos.SI);
        vinculo.setBloqueado(Codigos.NO);

        vehiculo = new GdVehiculo();
        vehiculo.setId(5L);
        vehiculo.setCasa(casa);
        vehiculo.setPlaca("ABC123");
        vehiculo.setActivo(Codigos.SI);
        vehiculo.setBloqueado(Codigos.NO);

        lenient().when(credencialQrService.resolver("QR")).thenReturn(Optional.of(credencial));
        lenient().when(residenteCasaRepository.findFirstByPersonaIdOrderByIdAsc(50L))
                .thenReturn(Optional.of(vinculo));
        lenient().when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(vehiculo));
        lenient().when(vehiculoRepository.operativosDeLaCasa(anyLong()))
                .thenReturn(Collections.singletonList(vehiculo));
        lenient().when(fabrica.nuevoEvento(any(), any()))
                .thenAnswer(inv -> new GdAccesoEvento());
        lenient().when(fabrica.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(fabrica.mapear(any())).thenAnswer(inv -> {
            GdAccesoEvento e = inv.getArgument(0);
            return AccesoEventoResponse.builder()
                    .sentido(e.getSentido())
                    .resultado(e.getResultado())
                    .motivoDenegacion(e.getMotivoDenegacion())
                    .vehiculoPlaca(e.getVehiculoPlaca())
                    .build();
        });
        lenient().when(fabrica.lecturaReciente(anyLong())).thenReturn(Optional.empty());
    }

    private VerificarQrRequest verificar() {
        VerificarQrRequest request = new VerificarQrRequest();
        request.setPayload("QR");
        return request;
    }

    private RegistrarAccesoRequest registrarEnVehiculo() {
        RegistrarAccesoRequest request = new RegistrarAccesoRequest();
        request.setPayload("QR");
        request.setModo(Codigos.MODO_VEHICULO);
        request.setVehiculoId(5L);
        return request;
    }

    // ── Persona ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("persona bloqueada por la administracion: motivo propio, distinto de inactiva")
    void personaBloqueadaTieneMotivoPropio() {
        persona.setBloqueado(Codigos.SI);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificar(), guardia);

        assertThat(ficha.isPermitido()).isFalse();
        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_PERSONA_BLOQUEADA);
        assertThat(ficha.getMensaje()).isEqualTo(MensajesGlobales.PERSONA_BLOQUEADA);
    }

    @Test
    @DisplayName("el bloqueo se evalua ANTES que el apagado: el mensaje no cambia si el residente reactiva")
    void bloqueoGanaSobreInactivo() {
        persona.setBloqueado(Codigos.SI);
        persona.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificar(), guardia);

        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_PERSONA_BLOQUEADA);
    }

    @Test
    @DisplayName("casa bloqueada: motivo propio")
    void casaBloqueadaTieneMotivoPropio() {
        casa.setBloqueado(Codigos.SI);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificar(), guardia);

        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_CASA_BLOQUEADA);
    }

    @Test
    @DisplayName("apagar el propio vinculo NO evade el bloqueo de la casa")
    void vinculoApagadoNoEvadeElBloqueoDeCasa() {
        casa.setBloqueado(Codigos.SI);
        // El residente apaga su vinculo desde el celular para "quedarse sin casa".
        vinculo.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        FichaVerificacionResponse ficha = servicio.verificar(verificar(), guardia);

        assertThat(ficha.isPermitido()).isFalse();
        assertThat(ficha.getMotivoDenegacion()).isEqualTo(Codigos.MOTIVO_CASA_BLOQUEADA);
    }

    // ── Vehiculo: la regla que NO cede ───────────────────────────────────────

    @Test
    @DisplayName("un vehiculo bloqueado NO sale, aunque la familia lo tenga encendido")
    void vehiculoBloqueadoNoSale() {
        vehiculo.setBloqueado(Codigos.SI);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(registrarEnVehiculo(), guardia))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.VEHICULO_BLOQUEADO);
    }

    @Test
    @DisplayName("el bloqueo del vehiculo tampoco cede cuando la persona esta ADENTRO")
    void vehiculoBloqueadoTampocoSaleEstandoAdentro() {
        vehiculo.setBloqueado(Codigos.SI);
        // "Quien esta adentro siempre puede salir" aplica a la PERSONA, no al
        // carro: sale a pie y el vehiculo se queda.
        when(presenciaService.estaAdentro(50L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(registrarEnVehiculo(), guardia))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.VEHICULO_BLOQUEADO);
    }

    @Test
    @DisplayName("con el vehiculo bloqueado, salir A PIE sigue siendo posible")
    void aPieSiSalePeseAlVehiculoBloqueado() {
        vehiculo.setBloqueado(Codigos.SI);
        when(presenciaService.estaAdentro(50L)).thenReturn(true);

        RegistrarAccesoRequest aPie = new RegistrarAccesoRequest();
        aPie.setPayload("QR");
        aPie.setModo(Codigos.MODO_PEATON);

        AccesoEventoResponse evento = servicio.registrar(aPie, guardia);

        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
        assertThat(evento.getSentido()).isEqualTo(Codigos.SALIDA);
    }

    @Test
    @DisplayName("la ficha NO ofrece placas que el registro va a rechazar")
    void laFichaSoloListaVehiculosOperativos() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);
        // El repositorio ya filtra activo='S' y bloqueado='N'.
        when(vehiculoRepository.operativosDeLaCasa(3L)).thenReturn(Collections.emptyList());

        FichaVerificacionResponse ficha = servicio.verificar(verificar(), guardia);

        assertThat(ficha.isPermitido()).isTrue();
        assertThat(ficha.getVehiculos()).isEmpty();
    }

    @Test
    @DisplayName("vehiculo apagado por la familia tampoco sale")
    void vehiculoApagadoNoSale() {
        vehiculo.setActivo(Codigos.NO);
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(registrarEnVehiculo(), guardia))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("sin bloqueos, el vehiculo del nucleo sale con cualquier miembro")
    void vehiculoDelNucleoSaleNormalmente() {
        when(presenciaService.estaAdentro(50L)).thenReturn(false);

        AccesoEventoResponse evento = servicio.registrar(registrarEnVehiculo(), guardia);

        assertThat(evento.getResultado()).isEqualTo(Codigos.RESULTADO_PERMITIDO);
        assertThat(evento.getVehiculoPlaca()).isEqualTo("ABC123");
    }
}
