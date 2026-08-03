package guardian.service.notificacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Cuando el correo se considera configurado, y que pasa cuando no lo esta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorreoServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private CorreoServiceImpl servicio(String host, String usuario, String clave) {
        return new CorreoServiceImpl(mailSender, host, usuario, clave,
                "no-responder@guardian.local", "GUARDIAN");
    }

    @Test
    @DisplayName("con host pero SIN clave NO se considera configurado")
    void configuracionAMediasNoCuenta() {
        // Es el estado normal mientras alguien tramita la contrasena de
        // aplicacion de Gmail. Mirando solo el host, el envio moria en un
        // "535 Username and Password not accepted" y el codigo dejaba de
        // escribirse en el log: la configuracion a medias rompia el unico
        // camino que quedaba para probar el flujo.
        assertThat(servicio("smtp.gmail.com", "cuenta@gmail.com", "").estaConfigurado())
                .isFalse();
        assertThat(servicio("smtp.gmail.com", "", "clave").estaConfigurado())
                .isFalse();
        assertThat(servicio("", "cuenta@gmail.com", "clave").estaConfigurado())
                .isFalse();
    }

    @Test
    @DisplayName("con los tres datos si se considera configurado")
    void configuracionCompleta() {
        assertThat(servicio("smtp.gmail.com", "cuenta@gmail.com", "clave").estaConfigurado())
                .isTrue();
    }

    @Test
    @DisplayName("sin configurar, el codigo va al log y NO se intenta enviar")
    void sinConfigurarNoIntentaEnviar() {
        servicio("smtp.gmail.com", "cuenta@gmail.com", "")
                .enviarCodigoRecuperacion("ana@correo.com", "Ana", "123456", 10);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("un fallo del SMTP NO se propaga")
    void elFalloNoSePropaga() {
        // Quien llama esta respondiendo "si ese documento existe, te llego un
        // codigo" — una respuesta que a proposito no revela nada. Propagar el
        // fallo la volveria delatora: error significaria "existe".
        doThrow(new MailSendException("Mail server connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> servicio("smtp.gmail.com", "cuenta@gmail.com", "clave")
                .enviarCodigoRecuperacion("ana@correo.com", "Ana", "123456", 10))
                .doesNotThrowAnyException();
    }
}
