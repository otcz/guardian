package guardian.service.notificacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Slf4j
@Service
public class CorreoServiceImpl implements CorreoService {

    private final JavaMailSender mailSender;
    private final String host;
    private final String usuario;
    private final String clave;
    private final String remitente;
    private final String nombreRemitente;

    public CorreoServiceImpl(JavaMailSender mailSender,
                             @Value("${spring.mail.host}") String host,
                             @Value("${spring.mail.username}") String usuario,
                             @Value("${spring.mail.password}") String clave,
                             @Value("${guardian.correo.remitente}") String remitente,
                             @Value("${guardian.correo.nombre-remitente}") String nombreRemitente) {
        this.mailSender = mailSender;
        this.host = host;
        this.usuario = usuario;
        this.clave = clave;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    /**
     * Exige host Y credenciales, no solo el host.
     *
     * <p>Con el host puesto y la clave todavia vacia —el estado normal
     * mientras alguien tramita la contrasena de aplicacion de Gmail— mirar
     * solo el host daba por configurado el correo, el envio moria en un
     * "535 Username and Password not accepted", y el codigo dejaba de
     * escribirse en el log. Es decir: la configuracion a medias rompia el
     * unico camino que quedaba para probar el flujo.</p>
     */
    @Override
    public boolean estaConfigurado() {
        return tiene(host) && tiene(usuario) && tiene(clave);
    }

    private boolean tiene(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    @Override
    public void enviarCodigoRecuperacion(String destinatario, String nombre, String codigo,
                                         int minutosVigencia) {
        if (!estaConfigurado()) {
            // Sin SMTP el flujo completo sigue siendo probable en desarrollo.
            // El codigo va al log y NO a la respuesta HTTP: ponerlo ahi seria
            // regalarle la cuenta a cualquiera que sepa un numero de documento.
            log.warn("[correo] SIN SMTP configurado ({}). Codigo para {}: {}",
                    queFalta(), destinatario, codigo);
            return;
        }

        enviar(destinatario, MensajeCorreo
                .de("Tu código para entrar a GUARDIAN",
                        "Recupera el acceso a tu cuenta",
                        "Hola " + nombre + ",")
                .parrafo("Alguien pidió recuperar el PIN de tu cuenta. "
                        + "Usa este código para elegir uno nuevo.")
                .destacado("Tu código", codigo)
                .parrafo("Vence en " + minutosVigencia
                        + " minutos y sirve una sola vez.")
                .advertencia("¿No lo pediste? Ignora este mensaje: tu PIN sigue "
                        + "siendo el mismo y nadie más puede usar este código."));
    }

    @Override
    public void enviar(String destinatario, MensajeCorreo mensaje) {
        // Sin destinatario no hay nada que hacer, y no es un error: la mitad de
        // las personas de un conjunto —los niños, los que solo pasan por la
        // portería— no tienen correo, y eso es legítimo.
        if (!tiene(destinatario)) {
            return;
        }
        if (!estaConfigurado()) {
            log.warn("[correo] SIN SMTP configurado ({}). No se envió a {}: {}",
                    queFalta(), destinatario, mensaje.getAsunto());
            return;
        }
        despachar(destinatario, mensaje);
    }

    /**
     * El envío de verdad, en multiparte.
     *
     * <p>MimeMessage y no SimpleMailMessage: el segundo solo sabe de texto
     * plano. El "true" del helper abre la multiparte y el UTF-8 explícito es lo
     * que hace que una eñe llegue como eñe.</p>
     */
    private void despachar(String destinatario, MensajeCorreo mensaje) {
        try {
            MimeMessage correo = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(correo, true, "UTF-8");

            helper.setFrom(new InternetAddress(remitente, nombreRemitente, "UTF-8"));
            helper.setTo(destinatario);
            helper.setSubject(mensaje.getAsunto());
            // El orden importa: primero el texto plano, después el HTML. Así lo
            // espera el estándar de multipart/alternative — el cliente muestra
            // la ÚLTIMA rama que entienda.
            helper.setText(PlantillaCorreo.texto(mensaje, nombreRemitente),
                    PlantillaCorreo.html(mensaje, nombreRemitente));

            mailSender.send(correo);
            log.info("[correo] enviado: {}", mensaje.getAsunto());
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException fallo) {
            // Se traga a proposito: ver el contrato en CorreoService. El log
            // queda para que el administrador pueda diagnosticar un SMTP mal
            // configurado, que si no seria invisible.
            //
            // El MENSAJE en la linea y la traza solo en debug: lo que resuelve
            // el problema es leer "535 Username and Password not accepted", no
            // ciento veinte lineas de pila que lo entierran.
            log.error("[correo] falló el envío de '{}': {}",
                    mensaje.getAsunto(), causaRaiz(fallo));
            log.debug("[correo] detalle del fallo", fallo);
        }
    }

    /** Para que el log diga QUE falta, y no solo que falta algo. */
    private String queFalta() {
        StringBuilder falta = new StringBuilder();
        if (!tiene(host)) {
            falta.append("falta GUARDIAN_SMTP_HOST ");
        }
        if (!tiene(usuario)) {
            falta.append("falta GUARDIAN_SMTP_USUARIO ");
        }
        if (!tiene(clave)) {
            falta.append("falta GUARDIAN_SMTP_CLAVE ");
        }
        return falta.toString().trim();
    }

    /**
     * El mensaje util vive al fondo de la cadena: Spring envuelve el fallo de
     * JavaMail y la linea de arriba solo dice "Mail server connection failed".
     */
    private String causaRaiz(Throwable fallo) {
        Throwable actual = fallo;
        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }
        return actual.getMessage() != null
                ? actual.getMessage().replaceAll("\\s+", " ").trim()
                : actual.getClass().getSimpleName();
    }

}
