package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.invitacion.InvitacionPublicaResponse;
import guardian.dto.invitacion.InvitacionRequest;
import guardian.entity.acceso.GdInvitacion;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.exception.GuardianException;
import guardian.repository.GdInvitacionRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.security.UsuarioAutenticado;
import guardian.util.HmacUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Ciclo de la invitacion: topes de vigencia, normalizacion de placa, firma del
 * payload GRDI y que la pagina publica no reparta codigos muertos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitacionServiceImplTest {

    private static final String SECRETO = "secreto-de-pruebas-con-32-caracteres!";
    private static final long DIA_MILIS = 86_400_000L;

    @Mock private GdInvitacionRepository invitacionRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private GdPersonaRepository personaRepository;

    @InjectMocks
    private InvitacionServiceImpl servicio;

    private UsuarioAutenticado anfitrion;
    private GdCasa casa;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "secretoHmac", SECRETO);
        ReflectionTestUtils.setField(servicio, "diasVigenciaMaxima", 30L);
        ReflectionTestUtils.setField(servicio, "usosMaximosTope", 20);

        anfitrion = new UsuarioAutenticado(1L, 50L, 1L, "123", "Ana Diaz", "RESIDENTE");

        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);
        casa = new GdCasa();
        casa.setId(3L);
        casa.setConjunto(conjunto);
        casa.setIdentificador("M1-C5");

        GdPersona persona = new GdPersona();
        persona.setId(50L);
        persona.setNombres("Ana");
        persona.setApellidos("Diaz");

        GdResidenteCasa vinculo = new GdResidenteCasa();
        vinculo.setPersona(persona);
        vinculo.setCasa(casa);

        lenient().when(residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(50L, Codigos.SI))
                .thenReturn(Optional.of(vinculo));
        lenient().when(personaRepository.findById(50L)).thenReturn(Optional.of(persona));
        lenient().when(invitacionRepository.save(any())).thenAnswer(inv -> {
            GdInvitacion i = inv.getArgument(0);
            i.setId(20L);
            return i;
        });
    }

    private InvitacionRequest requestBase() {
        InvitacionRequest request = new InvitacionRequest();
        request.setNombreInvitado("Pedro Perez");
        request.setDocumentoInvitado("999");
        return request;
    }

    // ── Creacion y topes ─────────────────────────────────────────────────────

    @Test
    @DisplayName("sin fechas: desde ahora hasta la medianoche, 1 uso")
    void defaultsDeCreacion() {
        InvitacionRequest request = requestBase();

        servicio.crear(request, anfitrion);

        // El save capturo la invitacion; el mock le puso id 20.
        // Los defaults son responsabilidad del service, no del cliente.
        org.mockito.ArgumentCaptor<GdInvitacion> captor =
                org.mockito.ArgumentCaptor.forClass(GdInvitacion.class);
        org.mockito.Mockito.verify(invitacionRepository).save(captor.capture());
        GdInvitacion creada = captor.getValue();

        assertThat(creada.getUsosMaximos()).isEqualTo(1);
        assertThat(creada.getVigenciaHasta()).isAfter(creada.getVigenciaDesde());
    }

    @Test
    @DisplayName("una invitacion que nace vencida se rechaza")
    void rechazaInvitacionEnElPasado() {
        InvitacionRequest request = requestBase();
        request.setVigenciaDesde(new Date(System.currentTimeMillis() - 2 * DIA_MILIS));
        request.setVigenciaHasta(new Date(System.currentTimeMillis() - DIA_MILIS));

        assertThatThrownBy(() -> servicio.crear(request, anfitrion))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.INVITACION_EN_PASADO);
    }

    @Test
    @DisplayName("la vigencia no puede superar el tope: 'efimero' es una promesa")
    void rechazaVigenciaMayorAlTope() {
        InvitacionRequest request = requestBase();
        request.setVigenciaDesde(new Date());
        request.setVigenciaHasta(new Date(System.currentTimeMillis() + 31 * DIA_MILIS));

        assertThatThrownBy(() -> servicio.crear(request, anfitrion))
                .hasMessage(MensajesGlobales.INVITACION_MUY_LARGA);
    }

    @Test
    @DisplayName("la placa se normaliza y una placa vacia queda como 'a pie'")
    void normalizaLaPlaca() {
        InvitacionRequest request = requestBase();
        request.setPlaca(" ab c-12 ");

        org.mockito.ArgumentCaptor<GdInvitacion> captor =
                org.mockito.ArgumentCaptor.forClass(GdInvitacion.class);
        servicio.crear(request, anfitrion);
        org.mockito.Mockito.verify(invitacionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlaca()).isEqualTo("ABC12");

        // Solo espacios y guiones NO es una placa.
        org.mockito.Mockito.reset(invitacionRepository);
        lenient().when(invitacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        InvitacionRequest sinPlaca = requestBase();
        sinPlaca.setPlaca(" - - ");
        servicio.crear(sinPlaca, anfitrion);
        org.mockito.Mockito.verify(invitacionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlaca()).isNull();
    }

    // ── Firma y resolucion ───────────────────────────────────────────────────

    @Test
    @DisplayName("el payload GRDI resuelve con firma valida y muere con firma alterada")
    void resuelveSoloConFirmaValida() {
        GdInvitacion invitacion = new GdInvitacion();
        invitacion.setCodigoPublico("codigo-x");
        invitacion.setFirmaHash(HmacUtil.firmar("codigo-x", SECRETO));
        when(invitacionRepository.buscarPorCodigoPublico("codigo-x"))
                .thenReturn(Optional.of(invitacion));

        String payload = "GRDI.codigo-x." + HmacUtil.firmar("codigo-x", SECRETO);
        assertThat(servicio.resolver(payload)).isPresent();

        String alterado = "GRDI.codigo-x." + HmacUtil.firmar("codigo-x", SECRETO + "x");
        assertThat(servicio.resolver(alterado)).isEmpty();

        assertThat(servicio.resolver("GRD1.codigo-x.firma")).isEmpty();
        assertThat(servicio.resolver(null)).isEmpty();
    }

    // ── Pagina publica ───────────────────────────────────────────────────────

    private GdInvitacion invitacionVigente() {
        GdPersona anfitrionPersona = new GdPersona();
        anfitrionPersona.setNombres("Ana");
        anfitrionPersona.setApellidos("Diaz");

        GdInvitacion invitacion = new GdInvitacion();
        invitacion.setCasa(casa);
        invitacion.setAnfitrion(anfitrionPersona);
        invitacion.setNombreInvitado("Pedro Perez");
        invitacion.setDocumentoInvitado("999");
        invitacion.setActivo(Codigos.SI);
        invitacion.setVigenciaDesde(new Date(System.currentTimeMillis() - DIA_MILIS));
        invitacion.setVigenciaHasta(new Date(System.currentTimeMillis() + DIA_MILIS));
        invitacion.setUsosMaximos(1);
        invitacion.setUsosRealizados(0);
        invitacion.setCodigoPublico("cod");
        invitacion.setFirmaHash("firma");
        return invitacion;
    }

    @Test
    @DisplayName("la pagina publica entrega el payload mientras la invitacion sirva")
    void publicaEntregaPayloadVigente() {
        when(invitacionRepository.buscarPorCodigoPublico("cod"))
                .thenReturn(Optional.of(invitacionVigente()));

        InvitacionPublicaResponse publica = servicio.publica("cod");

        assertThat(publica.getPayload()).isNotNull();
        assertThat(publica.getEstado()).isEqualTo("VIGENTE");
    }

    @Test
    @DisplayName("revocada: la pagina publica deja de repartir el codigo y el anfitrion")
    void publicaNoEntregaPayloadRevocada() {
        GdInvitacion revocada = invitacionVigente();
        revocada.setActivo(Codigos.NO);
        when(invitacionRepository.buscarPorCodigoPublico("cod"))
                .thenReturn(Optional.of(revocada));

        InvitacionPublicaResponse publica = servicio.publica("cod");

        assertThat(publica.getPayload()).isNull();
        assertThat(publica.getAnfitrionNombre()).isNull();
        assertThat(publica.getEstado()).isEqualTo("REVOCADA");
    }
}
