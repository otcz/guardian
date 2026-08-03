package guardian.service.notificacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(String.format("%s <%s>", nombreRemitente, remitente));
            mensaje.setTo(destinatario);
            mensaje.setSubject("Tu codigo para recuperar la contrasena");
            mensaje.setText(cuerpo(nombre, codigo, minutosVigencia));

            mailSender.send(mensaje);
            log.info("[correo] codigo de recuperacion enviado");
        } catch (RuntimeException fallo) {
            // Se traga a proposito: ver el contrato en CorreoService. El log
            // queda para que el administrador pueda diagnosticar un SMTP mal
            // configurado, que si no seria invisible.
            //
            // El MENSAJE en la linea y la traza solo en debug: lo que resuelve
            // el problema es leer "535 Username and Password not accepted", no
            // ciento veinte lineas de pila que lo entierran.
            log.error("[correo] fallo el envio: {}", causaRaiz(fallo));
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

    /**
     * Texto plano y no HTML. Un correo corto de seis digitos no gana nada con
     * maquetacion, y el texto plano llega igual a todos los clientes y cae
     * menos en spam que un HTML con imagenes.
     */
    private String cuerpo(String nombre, String codigo, int minutos) {
        return "Hola " + nombre + ",\n\n"
                + "Tu codigo para recuperar la contrasena es:\n\n"
                + "    " + codigo + "\n\n"
                + "Vence en " + minutos + " minutos y sirve una sola vez.\n\n"
                + "Si no pediste este codigo, ignora este mensaje: tu contrasena\n"
                + "sigue siendo la misma.\n\n"
                + "-- \n" + nombreRemitente + "\n"
                + "Este es un mensaje automatico. No respondas a este correo.";
    }
}
