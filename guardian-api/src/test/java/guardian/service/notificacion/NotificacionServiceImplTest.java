package guardian.service.notificacion;

import guardian.constant.Codigos;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.vehiculo.GdSolicitudVehiculo;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.repository.GdResidenteCasaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Los avisos de lo que decide la administracion.
 *
 * <p>Lo que se cubre es lo que puede hacer dano: que el aviso NO tumbe la
 * operacion que lo disparo, que no se mande a quien no tiene correo, y que el
 * bloqueo llegue a las dos personas que tienen que enterarse sin duplicarse
 * cuando son la misma.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacionServiceImplTest {

    @Mock private CorreoService correoService;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;

    private NotificacionServiceImpl servicio;

    private GdCasa casa;
    private GdPersona titular;

    @BeforeEach
    void preparar() {
        // Sin URL de aplicacion: es el estado por defecto y el que no debe
        // escribir la linea "Miralo en ...".
        servicio = new NotificacionServiceImpl(correoService, residenteCasaRepository, "");

        casa = new GdCasa();
        casa.setId(9L);
        casa.setIdentificador("CASA-B-52");

        titular = persona(1L, "OSCAR", "CARRILLO", "titular@correo.com");

        GdResidenteCasa vinculo = new GdResidenteCasa();
        vinculo.setPersona(titular);
        vinculo.setCasa(casa);
        vinculo.setParentesco(Codigos.PARENTESCO_TITULAR);

        lenient().when(residenteCasaRepository.findFirstByCasaIdAndParentescoAndActivo(
                9L, Codigos.PARENTESCO_TITULAR, Codigos.SI)).thenReturn(Optional.of(vinculo));
        lenient().when(residenteCasaRepository.findFirstByPersonaIdAndActivoOrderByIdAsc(
                any(), anyString())).thenReturn(Optional.of(vinculo));
    }

    @Test
    @DisplayName("un vehiculo aprobado le avisa a quien lo pidio")
    void vehiculoAprobadoAvisaAlSolicitante() {
        servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), true);

        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(destino.capture(), asunto.capture(), cuerpo.capture());

        assertThat(destino.getValue()).isEqualTo("titular@correo.com");
        assertThat(asunto.getValue()).contains("ABC123").contains("autorizado");
        assertThat(cuerpo.getValue()).contains("CASA-B-52");
        // Sin URL configurada, la invitacion a "mirarlo" no aparece.
        assertThat(cuerpo.getValue()).doesNotContain("Miralo en");
    }

    @Test
    @DisplayName("un rechazo sin motivo lo dice, en vez de dejar el hueco")
    void rechazoSinMotivoLoDice() {
        servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), false);

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(anyString(), anyString(), cuerpo.capture());

        // Un rechazo mudo deja a la persona sin nada que corregir, y la manda a
        // volver a pedir exactamente lo mismo.
        assertThat(cuerpo.getValue()).contains("No dejaron un motivo registrado");
    }

    @Test
    @DisplayName("el motivo del rechazo viaja tal cual lo escribio la administracion")
    void rechazoConMotivoLoIncluye() {
        servicio.solicitudVehiculoResuelta(
                solicitudVehiculo("ABC123", "La placa no coincide con la foto"), false);

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(anyString(), anyString(), cuerpo.capture());

        assertThat(cuerpo.getValue()).contains("La placa no coincide con la foto");
    }

    @Test
    @DisplayName("sin correo no se intenta enviar nada")
    void sinCorreoNoEnvia() {
        // No es un error: mucha gente del conjunto —los ninos, los que solo
        // pasan por la porteria— no tiene correo, y se entera al abrir la
        // aplicacion.
        GdSolicitudVehiculo solicitud = solicitudVehiculo("ABC123", null);
        solicitud.getSolicitante().setEmail(null);

        servicio.solicitudVehiculoResuelta(solicitud, true);

        verify(correoService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("bloquear a alguien le avisa a esa persona Y al titular")
    void bloqueoAvisaALosDos() {
        GdPersona hijo = persona(2L, "JUAN", "CARRILLO", "hijo@correo.com");

        servicio.bloqueoPersonaCambiado(hijo, true, "Perdio la credencial");

        // Dos avisos: quien se va a encontrar la talanquera abajo, y quien
        // responde por el hogar y a quien le van a reclamar.
        verify(correoService, times(2)).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("si el bloqueado ES el titular, el aviso sale una sola vez")
    void bloqueoDelTitularNoSeDuplica() {
        servicio.bloqueoPersonaCambiado(titular, true, "Motivo cualquiera");

        verify(correoService, times(1)).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("un SMTP caido NO tumba la operacion que disparo el aviso")
    void unFalloDeCorreoNoRompeNada() {
        // Es la garantia que sostiene todo lo demas: cuando esto se llama, la
        // aprobacion o el bloqueo YA ocurrieron. Si un correo pudiera propagar
        // su excepcion, el conjunto se quedaria sin poder administrarse cada
        // vez que Google tenga un mal dia.
        doThrow(new RuntimeException("535 Username and Password not accepted"))
                .when(correoService).enviar(anyString(), anyString(), anyString());

        assertThatCode(() ->
                servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("con URL configurada, el aviso dice a donde ir")
    void conUrlIncluyeElEnlace() {
        servicio = new NotificacionServiceImpl(
                correoService, residenteCasaRepository, "https://guardiaco.com");

        servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), true);

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(anyString(), anyString(), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("https://guardiaco.com");
    }

    @Test
    @DisplayName("un vehiculo deshabilitado le avisa al titular de su casa")
    void vehiculoBloqueadoAvisaAlTitular() {
        GdVehiculo vehiculo = new GdVehiculo();
        vehiculo.setId(5L);
        vehiculo.setPlaca("XYZ789");
        vehiculo.setCasa(casa);

        servicio.bloqueoVehiculoCambiado(vehiculo, true, "Entro por la porteria equivocada");

        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(destino.capture(), anyString(), cuerpo.capture());

        assertThat(destino.getValue()).isEqualTo("titular@correo.com");
        assertThat(cuerpo.getValue())
                .contains("XYZ789")
                .contains("Entro por la porteria equivocada")
                // Lo que el titular necesita saber: que no lo puede resolver el
                // solo desde el celular.
                .contains("Escribe a la administracion");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdPersona persona(Long id, String nombres, String apellidos, String email) {
        GdPersona p = new GdPersona();
        p.setId(id);
        p.setNombres(nombres);
        p.setApellidos(apellidos);
        p.setEmail(email);
        return p;
    }

    private GdSolicitudVehiculo solicitudVehiculo(String placa, String motivoRechazo) {
        GdSolicitudVehiculo s = new GdSolicitudVehiculo();
        s.setId(3L);
        s.setPlaca(placa);
        s.setCasa(casa);
        s.setSolicitante(titular);
        s.setMotivoRechazo(motivoRechazo);
        return s;
    }
}
