package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.residente.CodigoHogarResponse;
import guardian.dto.residente.RegistroHogarRequest;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdCodigoHogar;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdSolicitudHogar;
import guardian.exception.GuardianException;
import guardian.repository.GdCodigoHogarRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdSolicitudHogarRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.ParametroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El codigo con el que un familiar pide unirse a un hogar existente.
 *
 * <p>Usarlo NO crea a nadie: solo deja una {@link GdSolicitudHogar} PENDIENTE.
 * Quien de verdad crea la persona con credencial para entrar es
 * {@code SolicitudHogarAdminServiceImpl.aprobar}. Por eso todo lo que se
 * prueba aca es lo que acota el uso del codigo — un solo uso, vencimiento, y
 * que solo el titular pueda emitirlo — y NO la creacion de la persona.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CodigoHogarServiceImplTest {

    @Mock private GdCodigoHogarRepository codigoRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private GdPersonaRepository personaRepository;
    @Mock private GdSolicitudHogarRepository solicitudRepository;
    @Mock private ParametroService parametroService;

    @InjectMocks
    private CodigoHogarServiceImpl servicio;

    private GdCasa casa;
    private GdPersona titular;
    private UsuarioAutenticado sesionTitular;
    private UsuarioAutenticado sesionHijo;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "horasVigencia", 24L);

        GdConjunto sede = new GdConjunto();
        sede.setId(1L);
        sede.setNombre("Santa Barbara");

        casa = new GdCasa();
        casa.setId(5L);
        casa.setConjunto(sede);
        casa.setIdentificador("M1-C5");

        titular = new GdPersona();
        titular.setId(10L);
        titular.setConjunto(sede);
        titular.setDocumento("1074");
        titular.setNombres("Oscar");
        titular.setApellidos("Carrillo");

        sesionTitular = new UsuarioAutenticado(1L, 10L, 1L, "1074", "Oscar Carrillo",
                Codigos.ROL_RESIDENTE);
        sesionHijo = new UsuarioAutenticado(2L, 20L, 1L, "1075", "Tomas Carrillo",
                Codigos.ROL_RESIDENTE);

        lenient().when(personaRepository.findById(10L)).thenReturn(Optional.of(titular));
        lenient().when(personaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(codigoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        lenient().when(residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(any(), anyString()))
                .thenReturn(Optional.of(vinculo(titular, Codigos.PARENTESCO_TITULAR)));
        lenient().when(residenteCasaRepository.findByPersonaIdAndCasaId(10L, 5L))
                .thenReturn(Optional.of(vinculo(titular, Codigos.PARENTESCO_TITULAR)));
        lenient().when(residenteCasaRepository.findByPersonaIdAndCasaId(20L, 5L))
                .thenReturn(Optional.of(vinculo(titular, "HIJO")));
    }

    private GdResidenteCasa vinculo(GdPersona persona, String parentesco) {
        GdResidenteCasa v = new GdResidenteCasa();
        v.setPersona(persona);
        v.setCasa(casa);
        v.setParentesco(parentesco);
        v.setActivo(Codigos.SI);
        return v;
    }

    private GdCodigoHogar codigoVivo() {
        GdCodigoHogar c = new GdCodigoHogar();
        c.setCasa(casa);
        c.setTitular(titular);
        c.setCodigo("uuid-de-prueba");
        c.setVigenciaHasta(new Date(System.currentTimeMillis() + 3_600_000L));
        c.setActivo(Codigos.SI);
        c.setBloqueado(Codigos.NO);
        return c;
    }

    // ── Quien puede emitirlo ────────────────────────────────────────────────

    @Test
    @DisplayName("el titular genera un codigo con vencimiento")
    void elTitularGenera() {
        CodigoHogarResponse respuesta = servicio.generar(sesionTitular);

        assertThat(respuesta.getCodigo()).isNotBlank();
        assertThat(respuesta.isVigente()).isTrue();
        assertThat(respuesta.getVigenciaHasta()).isAfter(new Date());
    }

    @Test
    @DisplayName("un miembro que NO es titular no puede invitar")
    void soloElTitularInvita() {
        // Cualquier miembro podria meter gente en la casa de otro, y quien
        // responde por ese hogar es uno solo.
        assertThatThrownBy(() -> servicio.generar(sesionHijo))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.SOLO_TITULAR_FAMILIA);

        verify(codigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("generar uno nuevo invalida el anterior sin usar")
    void unoVivoALaVez() {
        GdCodigoHogar anterior = codigoVivo();
        when(codigoRepository.findFirstByCasaIdOrderByIdDesc(5L))
                .thenReturn(Optional.of(anterior));

        servicio.generar(sesionTitular);

        // Si quedaran varios, revocar el que se compartio por error no
        // serviria: los otros seguirian abiertos.
        assertThat(anterior.getActivo()).isEqualTo(Codigos.NO);
    }

    // ── Quien lo usa ────────────────────────────────────────────────────────

    @Test
    @DisplayName("registrarse deja una solicitud PENDIENTE, sin crear a nadie todavia")
    void registrarQuedaPendiente() {
        when(codigoRepository.findByCodigo("uuid-de-prueba"))
                .thenReturn(Optional.of(codigoVivo()));
        when(personaRepository.findByDocumento("1099")).thenReturn(Optional.empty());

        servicio.registrar("uuid-de-prueba", solicitud("1099", "HIJO"));

        ArgumentCaptor<GdSolicitudHogar> captura = ArgumentCaptor.forClass(GdSolicitudHogar.class);
        verify(solicitudRepository).save(captura.capture());
        GdSolicitudHogar guardada = captura.getValue();
        assertThat(guardada.getDocumento()).isEqualTo("1099");
        assertThat(guardada.getParentesco()).isEqualTo("HIJO");
        assertThat(guardada.getEstado()).isEqualTo(Codigos.SOLICITUD_PENDIENTE);

        // Nada de esto existe hasta que un administrador apruebe.
        verify(personaRepository, never()).save(any());
        verify(residenteCasaRepository, never()).save(any());
        // El codigo tampoco se quema en este paso.
        verify(codigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("un codigo con una solicitud pendiente no admite una segunda")
    void unaSolicitudPendienteALaVez() {
        when(codigoRepository.findByCodigo("uuid-de-prueba"))
                .thenReturn(Optional.of(codigoVivo()));
        when(solicitudRepository.existsByCodigoIdAndEstado(any(), eq(Codigos.SOLICITUD_PENDIENTE)))
                .thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar("uuid-de-prueba", solicitud("1100", "HIJO")))
                .hasMessage(MensajesGlobales.SOLICITUD_HOGAR_YA_PENDIENTE);

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("un codigo vencido no sirve")
    void vencidoNoSirve() {
        GdCodigoHogar viejo = codigoVivo();
        viejo.setVigenciaHasta(new Date(System.currentTimeMillis() - 1_000L));
        when(codigoRepository.findByCodigo("uuid-de-prueba")).thenReturn(Optional.of(viejo));

        assertThatThrownBy(() -> servicio.registrar("uuid-de-prueba", solicitud("1099", "HIJO")))
                .hasMessage(MensajesGlobales.CODIGO_HOGAR_NO_VALIDO);

        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un codigo revocado no sirve")
    void revocadoNoSirve() {
        GdCodigoHogar anulado = codigoVivo();
        anulado.setActivo(Codigos.NO);
        when(codigoRepository.findByCodigo("uuid-de-prueba")).thenReturn(Optional.of(anulado));

        assertThatThrownBy(() -> servicio.registrar("uuid-de-prueba", solicitud("1099", "HIJO")))
                .hasMessage(MensajesGlobales.CODIGO_HOGAR_NO_VALIDO);
    }

    @Test
    @DisplayName("nadie se registra como TITULAR: esa casa ya tiene uno")
    void noSePuedeEntrarComoTitular() {
        when(codigoRepository.findByCodigo("uuid-de-prueba"))
                .thenReturn(Optional.of(codigoVivo()));

        assertThatThrownBy(() -> servicio.registrar(
                "uuid-de-prueba", solicitud("1099", Codigos.PARENTESCO_TITULAR)))
                .hasMessage(MensajesGlobales.PARENTESCO_TITULAR_NO);

        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un documento que ya existe no se duplica")
    void documentoUnico() {
        when(codigoRepository.findByCodigo("uuid-de-prueba"))
                .thenReturn(Optional.of(codigoVivo()));
        when(personaRepository.findByDocumento("1074")).thenReturn(Optional.of(titular));

        assertThatThrownBy(() -> servicio.registrar("uuid-de-prueba", solicitud("1074", "HIJO")))
                .hasMessage(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);
    }

    @Test
    @DisplayName("la pantalla publica muestra a donde se une, y nada mas")
    void laConsultaPublicaNoFiltraDeMas() {
        when(codigoRepository.findByCodigo("uuid-de-prueba"))
                .thenReturn(Optional.of(codigoVivo()));

        assertThat(servicio.consultar("uuid-de-prueba").getCasaIdentificador()).isEqualTo("M1-C5");
        assertThat(servicio.consultar("uuid-de-prueba").getConjuntoNombre())
                .isEqualTo("Santa Barbara");
        assertThat(servicio.consultar("uuid-de-prueba").getTitularNombre())
                .isEqualTo("Oscar Carrillo");
    }

    private RegistroHogarRequest solicitud(String documento, String parentesco) {
        RegistroHogarRequest r = new RegistroHogarRequest();
        r.setDocumento(documento);
        r.setNombres("Tomas");
        r.setApellidos("Carrillo");
        r.setParentesco(parentesco);
        return r;
    }
}
