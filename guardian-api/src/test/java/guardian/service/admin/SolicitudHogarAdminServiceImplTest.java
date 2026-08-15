package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.persona.GdCodigoHogar;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdSolicitudHogar;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdCodigoHogarRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdSolicitudHogarRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.notificacion.NotificacionService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aprobar una solicitud de hogar es lo que de verdad CREA a la persona, su
 * cuenta y su vinculo con la casa — antes de esto no existe nada de eso.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudHogarAdminServiceImplTest {

    @Mock private GdSolicitudHogarRepository solicitudRepository;
    @Mock private GdCodigoHogarRepository codigoRepository;
    @Mock private GdPersonaRepository personaRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private GdUsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private SolicitudHogarAdminServiceImpl servicio;

    private UsuarioAutenticado admin;
    private GdCasa casa;
    private GdCodigoHogar codigo;

    @BeforeEach
    void preparar() {
        admin = new UsuarioAutenticado(1L, 1L, 1L, "ADMIN", "Admin", Codigos.ROL_ADMIN);

        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(1L);

        casa = new GdCasa();
        casa.setId(5L);
        casa.setConjunto(conjunto);
        casa.setIdentificador("M1-C5");

        GdPersona titular = new GdPersona();
        titular.setId(10L);
        titular.setNombres("Oscar");
        titular.setApellidos("Carrillo");

        codigo = new GdCodigoHogar();
        codigo.setId(3L);
        codigo.setCasa(casa);
        codigo.setTitular(titular);

        lenient().when(personaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(residenteCasaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(codigoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$hash");
    }

    private GdSolicitudHogar pendiente() {
        GdSolicitudHogar s = new GdSolicitudHogar();
        s.setId(7L);
        s.setCodigo(codigo);
        s.setDocumento("1099");
        s.setNombres("Yajaira");
        s.setApellidos("Cuadrado");
        s.setParentesco("ESPOSA");
        s.setEmail("yajaira@correo.com");
        s.setEstado(Codigos.SOLICITUD_PENDIENTE);
        return s;
    }

    @Test
    @DisplayName("aprobar crea persona, vinculo con la casa, cuenta y quema el codigo")
    void aprobarCreaTodo() {
        GdSolicitudHogar solicitud = pendiente();
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(personaRepository.findByDocumento("1099")).thenReturn(Optional.empty());

        servicio.aprobar(7L, admin);

        ArgumentCaptor<GdPersona> persona = ArgumentCaptor.forClass(GdPersona.class);
        verify(personaRepository).save(persona.capture());
        assertThat(persona.getValue().getDocumento()).isEqualTo("1099");

        ArgumentCaptor<GdResidenteCasa> vinculo = ArgumentCaptor.forClass(GdResidenteCasa.class);
        verify(residenteCasaRepository).save(vinculo.capture());
        assertThat(vinculo.getValue().getCasa().getId()).isEqualTo(5L);
        assertThat(vinculo.getValue().getParentesco()).isEqualTo("ESPOSA");

        ArgumentCaptor<GdUsuario> cuenta = ArgumentCaptor.forClass(GdUsuario.class);
        verify(usuarioRepository).save(cuenta.capture());
        assertThat(cuenta.getValue().getRol()).isEqualTo(Codigos.ROL_RESIDENTE);
        assertThat(cuenta.getValue().getActivo()).isEqualTo(Codigos.SI);

        ArgumentCaptor<GdCodigoHogar> codigoGuardado = ArgumentCaptor.forClass(GdCodigoHogar.class);
        verify(codigoRepository).save(codigoGuardado.capture());
        assertThat(codigoGuardado.getValue().estaUsado()).isTrue();

        assertThat(solicitud.getEstado()).isEqualTo(Codigos.SOLICITUD_APROBADA);
    }

    @Test
    @DisplayName("aprobar rechaza si el documento se registro por otro camino mientras esperaba")
    void aprobarRevalidaElDocumento() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(pendiente()));
        when(personaRepository.findByDocumento("1099"))
                .thenReturn(Optional.of(new GdPersona()));

        assertThatThrownBy(() -> servicio.aprobar(7L, admin))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.DOCUMENTO_YA_REGISTRADO);

        verify(personaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("una solicitud ya resuelta no se puede volver a aprobar")
    void noSeResuelveDosVeces() {
        GdSolicitudHogar resuelta = pendiente();
        resuelta.setEstado(Codigos.SOLICITUD_APROBADA);
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(resuelta));

        assertThatThrownBy(() -> servicio.aprobar(7L, admin))
                .hasMessage(MensajesGlobales.SOLICITUD_NO_PENDIENTE);
    }

    @Test
    @DisplayName("rechazar guarda el motivo y no toca el codigo ni crea a nadie")
    void rechazarNoCreaNiQuema() {
        GdSolicitudHogar solicitud = pendiente();
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));

        servicio.rechazar(7L, "El documento no coincide con la cedula", admin);

        assertThat(solicitud.getEstado()).isEqualTo(Codigos.SOLICITUD_RECHAZADA);
        assertThat(solicitud.getMotivoRechazo()).isEqualTo("El documento no coincide con la cedula");
        verify(personaRepository, never()).save(any());
        verify(codigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("una solicitud de otra sede no se puede resolver")
    void noSePuedeResolverDeOtraSede() {
        GdConjunto otraSede = new GdConjunto();
        otraSede.setId(99L);
        GdCasa casaAjena = new GdCasa();
        casaAjena.setId(50L);
        casaAjena.setConjunto(otraSede);
        codigo.setCasa(casaAjena);

        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(pendiente()));

        assertThatThrownBy(() -> servicio.aprobar(7L, admin))
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);
    }
}
