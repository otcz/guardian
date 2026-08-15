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
 * Los avisos de lo que decide la administración.
 *
 * <p>Lo que se cubre es lo que puede hacer daño: que el aviso NO tumbe la
 * operación que lo disparó, que no se mande a quien no tiene correo, y que el
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
        // Sin URL de aplicación: es el estado por defecto, y el que NO debe
        // dibujar el botón de acción.
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
    @DisplayName("un vehículo aprobado le avisa a quien lo pidió")
    void vehiculoAprobadoAvisaAlSolicitante() {
        servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), true);

        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService).enviar(destino.capture(), mensaje.capture());

        assertThat(destino.getValue()).isEqualTo("titular@correo.com");
        assertThat(mensaje.getValue().getAsunto()).contains("ABC123").contains("autorizado");
        // La placa va en el bloque destacado: es lo que se lee de un vistazo.
        assertThat(mensaje.getValue().getDestacado()).isEqualTo("ABC123");
        assertThat(texto(mensaje.getValue())).contains("CASA-B-52");
        // Sin URL configurada no se dibuja el botón, que si no llevaría a nada.
        assertThat(mensaje.getValue().tieneAccion()).isFalse();
    }

    @Test
    @DisplayName("un rechazo sin motivo lo dice, en vez de dejar el hueco")
    void rechazoSinMotivoLoDice() {
        servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), false);

        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService).enviar(anyString(), mensaje.capture());

        // Un rechazo mudo deja a la persona sin nada que corregir, y la manda a
        // volver a pedir exactamente lo mismo.
        assertThat(texto(mensaje.getValue())).contains("no dejó un motivo registrado");
    }

    @Test
    @DisplayName("el motivo del rechazo viaja tal cual lo escribió la administración")
    void rechazoConMotivoLoIncluye() {
        servicio.solicitudVehiculoResuelta(
                solicitudVehiculo("ABC123", "La placa no coincide con la foto"), false);

        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService).enviar(anyString(), mensaje.capture());

        assertThat(texto(mensaje.getValue())).contains("La placa no coincide con la foto");
    }

    @Test
    @DisplayName("sin correo no se intenta enviar nada")
    void sinCorreoNoEnvia() {
        // No es un error: mucha gente del conjunto —los niños, los que solo
        // pasan por la portería— no tiene correo, y se entera al abrir la
        // aplicación.
        GdSolicitudVehiculo solicitud = solicitudVehiculo("ABC123", null);
        solicitud.getSolicitante().setEmail(null);

        servicio.solicitudVehiculoResuelta(solicitud, true);

        verify(correoService, never()).enviar(anyString(), any());
    }

    @Test
    @DisplayName("bloquear a alguien le avisa a esa persona Y al titular")
    void bloqueoAvisaALosDos() {
        GdPersona hijo = persona(2L, "JUAN", "CARRILLO", "hijo@correo.com");

        servicio.bloqueoPersonaCambiado(hijo, true, "Perdió la credencial");

        // Dos avisos: quien se va a encontrar la talanquera abajo, y quien
        // responde por el hogar y a quien le van a reclamar.
        verify(correoService, times(2)).enviar(anyString(), any());
    }

    @Test
    @DisplayName("si el bloqueado ES el titular, el aviso sale una sola vez")
    void bloqueoDelTitularNoSeDuplica() {
        servicio.bloqueoPersonaCambiado(titular, true, "Motivo cualquiera");

        verify(correoService, times(1)).enviar(anyString(), any());
    }

    @Test
    @DisplayName("un SMTP caído NO tumba la operación que disparó el aviso")
    void unFalloDeCorreoNoRompeNada() {
        // Es la garantía que sostiene todo lo demás: cuando esto se llama, la
        // aprobación o el bloqueo YA ocurrieron. Si un correo pudiera propagar
        // su excepción, el conjunto se quedaría sin poder administrarse cada
        // vez que Google tenga un mal día.
        doThrow(new RuntimeException("535 Username and Password not accepted"))
                .when(correoService).enviar(anyString(), any());

        assertThatCode(() ->
                servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("con URL configurada, el aviso lleva botón a la aplicación")
    void conUrlIncluyeElBoton() {
        servicio = new NotificacionServiceImpl(
                correoService, residenteCasaRepository, "https://guardiaco.com");

        servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), true);

        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService).enviar(anyString(), mensaje.capture());

        assertThat(mensaje.getValue().tieneAccion()).isTrue();
        assertThat(mensaje.getValue().getUrlAccion()).isEqualTo("https://guardiaco.com");
    }

    @Test
    @DisplayName("un vehículo deshabilitado le avisa al titular de su casa")
    void vehiculoBloqueadoAvisaAlTitular() {
        GdVehiculo vehiculo = new GdVehiculo();
        vehiculo.setId(5L);
        vehiculo.setPlaca("XYZ789");
        vehiculo.setCasa(casa);

        servicio.bloqueoVehiculoCambiado(vehiculo, true, "Entró por la portería equivocada");

        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService).enviar(destino.capture(), mensaje.capture());

        assertThat(destino.getValue()).isEqualTo("titular@correo.com");
        assertThat(mensaje.getValue().getDestacado()).isEqualTo("XYZ789");
        assertThat(texto(mensaje.getValue()))
                .contains("Entró por la portería equivocada")
                // Lo que el titular necesita saber: que no lo puede resolver él
                // solo desde el celular.
                .contains("escribe a la administración");
    }

    @Test
    @DisplayName("las tildes y las eñes sobreviven a la plantilla")
    void elTextoConservaLosAcentos() {
        // El motivo por el que existe este test: todo el backend estaba en
        // ASCII porque el pom no declaraba el encoding de las fuentes, y a los
        // residentes les llegaba "contrasena". Si alguien vuelve a romper esa
        // configuración, esto lo caza antes de que salga un correo.
        GdVehiculo vehiculo = new GdVehiculo();
        vehiculo.setId(5L);
        vehiculo.setPlaca("XYZ789");
        vehiculo.setCasa(casa);

        servicio.bloqueoVehiculoCambiado(vehiculo, true, "Sin razón");

        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService).enviar(anyString(), mensaje.capture());

        assertThat(mensaje.getValue().getAsunto()).contains("vehículo");
        assertThat(texto(mensaje.getValue())).contains("administración");

        // Y que la plantilla no los rompa al maquetar.
        String html = PlantillaCorreo.html(mensaje.getValue(), "GUARDIAN");
        assertThat(html).contains("vehículo").contains("charset=\"utf-8\"");
    }

    @Test
    @DisplayName("lo que escribe la administración no puede inyectar HTML")
    void elMotivoSeEscapa() {
        // El motivo lo teclea una persona en un formulario. Sin escapar, un
        // fragmento de HTML pegado ahí se ejecutaría en el cliente de correo
        // de quien lo recibe.
        servicio.solicitudVehiculoResuelta(
                solicitudVehiculo("ABC123", "<script>alert(1)</script>"), false);

        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService).enviar(anyString(), mensaje.capture());

        String html = PlantillaCorreo.html(mensaje.getValue(), "GUARDIAN");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("si quien pidió el vehículo ES el titular, no le llegan dos correos")
    void elTitularNoRecibeDuplicado() {
        // Recibir dos avisos del mismo hecho enseña a ignorarlos, y el día que
        // llegue uno que importa ya nadie los abre.
        servicio.solicitudVehiculoResuelta(solicitudVehiculo("ABC123", null), true);

        verify(correoService, times(1)).enviar(anyString(), any());
    }

    @Test
    @DisplayName("aprobar un ingreso al hogar le avisa al titular, SIN el PIN del otro")
    void alTitularNoSeLeFiltraElPinAjeno() {
        // Dos correos distintos y no el mismo a dos destinos: el de la persona
        // nueva lleva su PIN inicial, y mandárselo al titular sería entregarle
        // una credencial que no es suya.
        servicio.solicitudHogarResuelta(solicitudHogar("nueva@correo.com"), true);

        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MensajeCorreo> mensaje = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correoService, times(2)).enviar(destino.capture(), mensaje.capture());

        int alTitular = destino.getAllValues().indexOf("titular@correo.com");
        assertThat(alTitular).isGreaterThanOrEqualTo(0);
        assertThat(texto(mensaje.getAllValues().get(alTitular)))
                .doesNotContain(Codigos.CLAVE_INICIAL)
                .contains("PRUEBA NUEVA");

        // Y a la persona nueva sí, que sin eso no puede entrar.
        int aLaPersona = destino.getAllValues().indexOf("nueva@correo.com");
        assertThat(texto(mensaje.getAllValues().get(aLaPersona)))
                .contains(Codigos.CLAVE_INICIAL);
    }

    @Test
    @DisplayName("si quien pide entrar al hogar usa el correo del titular, sale uno solo")
    void hogarConElMismoCorreoNoSeDuplica() {
        servicio.solicitudHogarResuelta(solicitudHogar("titular@correo.com"), true);

        verify(correoService, times(1)).enviar(anyString(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private guardian.entity.persona.GdSolicitudHogar solicitudHogar(String email) {
        guardian.entity.persona.GdCodigoHogar codigo =
                new guardian.entity.persona.GdCodigoHogar();
        codigo.setCasa(casa);

        guardian.entity.persona.GdSolicitudHogar s =
                new guardian.entity.persona.GdSolicitudHogar();
        s.setId(4L);
        s.setCodigo(codigo);
        s.setNombres("PRUEBA");
        s.setApellidos("NUEVA");
        s.setDocumento("111222333");
        s.setEmail(email);
        return s;
    }

    /** Todo lo redactado del mensaje, para buscar una frase sin importar dónde cayó. */
    private String texto(MensajeCorreo mensaje) {
        return PlantillaCorreo.texto(mensaje, "GUARDIAN");
    }

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
