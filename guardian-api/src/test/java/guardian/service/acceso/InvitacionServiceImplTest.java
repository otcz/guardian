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
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdInvitacionRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.security.UsuarioAutenticado;
import guardian.util.HmacUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock private GdAccesoEventoRepository eventoRepository;

    @InjectMocks
    private InvitacionServiceImpl servicio;

    private UsuarioAutenticado anfitrion;
    private GdCasa casa;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "secretoHmac", SECRETO);
        ReflectionTestUtils.setField(servicio, "diasVigenciaMaxima", 30L);

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
    @DisplayName("sin fechas: desde ahora hasta la medianoche, sin tope de entradas")
    void defaultsDeCreacion() {
        InvitacionRequest request = requestBase();

        servicio.crear(request, anfitrion);

        // El save capturo la invitacion; el mock le puso id 20.
        // Los defaults son responsabilidad del service, no del cliente.
        org.mockito.ArgumentCaptor<GdInvitacion> captor =
                org.mockito.ArgumentCaptor.forClass(GdInvitacion.class);
        org.mockito.Mockito.verify(invitacionRepository).save(captor.capture());
        GdInvitacion creada = captor.getValue();

        // Sin tope: la visita la acota su ventana, no un contador de entradas.
        assertThat(creada.getUsosMaximos()).isEqualTo(GdInvitacion.SIN_LIMITE);
        assertThat(creada.agotada()).isFalse();
        assertThat(creada.getVigenciaHasta()).isAfter(creada.getVigenciaDesde());
    }

    @Test
    @DisplayName("la invitacion nueva no se agota por entrar muchas veces")
    void sinTopeNoSeAgota() {
        GdInvitacion nueva = new GdInvitacion();
        nueva.setUsosMaximos(GdInvitacion.SIN_LIMITE);
        nueva.setUsosRealizados(37);

        assertThat(nueva.agotada()).isFalse();
    }

    @Test
    @DisplayName("las invitaciones viejas con tope se siguen respetando")
    void elTopeViejoSigueValiendo() {
        GdInvitacion vieja = new GdInvitacion();
        vieja.setUsosMaximos(1);
        vieja.setUsosRealizados(1);

        assertThat(vieja.agotada()).isTrue();
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

    // ── Eliminar: solo el super administrador ────────────────────────────────

    @Test
    @DisplayName("el residente y el administrador NO pueden eliminar")
    void borradoVedadoAlResidenteYAlAdmin() {
        // El punto de toda la regla: si el anfitrion pudiera borrar su propia
        // invitacion, desapareceria el registro de quien dejo entrar a alguien
        // — y eso es exactamente lo que una disputa necesita saber.
        GdInvitacion invitacion = invitacionVigente();
        invitacion.setId(20L);
        invitacion.setConjuntoId(1L);
        lenient().when(invitacionRepository.findById(20L)).thenReturn(Optional.of(invitacion));

        UsuarioAutenticado admin = new UsuarioAutenticado(2L, 60L, 1L, "1074", "Admin", "ADMIN");

        assertThatThrownBy(() -> servicio.eliminarComoSuperAdmin(20L, anfitrion))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.BORRADO_SOLO_SUPER_ADMIN);
        assertThatThrownBy(() -> servicio.eliminarComoSuperAdmin(20L, admin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.BORRADO_SOLO_SUPER_ADMIN);

        verify(invitacionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("el super administrador elimina, y la bitacora se desvincula antes de borrar")
    void superAdminEliminaConservandoLaBitacora() {
        // Los ingresos que esa invitacion permitio son del conjunto. El evento
        // guarda nombre y documento del invitado copiados encima, asi que
        // sobrevive sin la fila — pero el orden importa: al reves, la FK
        // rechaza el borrado.
        GdInvitacion invitacion = invitacionVigente();
        invitacion.setId(20L);
        invitacion.setConjuntoId(1L);
        when(invitacionRepository.findById(20L)).thenReturn(Optional.of(invitacion));

        servicio.eliminarComoSuperAdmin(20L, superAdmin(1L));

        InOrder orden = inOrder(eventoRepository, invitacionRepository);
        orden.verify(eventoRepository).desvincularInvitacion(20L);
        orden.verify(invitacionRepository).delete(invitacion);
    }

    @Test
    @DisplayName("ni el super administrador elimina invitaciones de otra sede")
    void noEliminaDeOtraSede() {
        // El filtro por conjunto es lo unico que separa una sede de otra
        // mientras esta adentro: sin el, un id adivinado borraria en la sede
        // equivocada.
        GdInvitacion invitacion = invitacionVigente();
        invitacion.setId(22L);
        invitacion.setConjuntoId(1L);
        when(invitacionRepository.findById(22L)).thenReturn(Optional.of(invitacion));

        assertThatThrownBy(() -> servicio.eliminarComoSuperAdmin(22L, superAdmin(7L)))
                .isInstanceOf(GuardianException.class);

        verify(invitacionRepository, never()).delete(any());
    }

    /** Operador de la plataforma, ya metido en la sede indicada. */
    private UsuarioAutenticado superAdmin(Long conjuntoId) {
        return new UsuarioAutenticado(9L, 90L, conjuntoId, "SUPERADMIN", "Plataforma",
                Codigos.ROL_SUPER_ADMIN);
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
