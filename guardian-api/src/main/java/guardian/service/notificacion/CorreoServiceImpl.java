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
    private final String remitente;
    private final String nombreRemitente;

    public CorreoServiceImpl(JavaMailSender mailSender,
                             @Value("${spring.mail.host}") String host,
                             @Value("${guardian.correo.remitente}") String remitente,
                             @Value("${guardian.correo.nombre-remitente}") String nombreRemitente) {
        this.mailSender = mailSender;
        this.host = host;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    @Override
    public boolean estaConfigurado() {
        return host != null && !host.trim().isEmpty();
    }

    @Override
    public void enviarCodigoRecuperacion(String destinatario, String nombre, String codigo,
                                         int minutosVigencia) {
        if (!estaConfigurado()) {
            // Sin SMTP el flujo completo sigue siendo probable en desarrollo.
            // El codigo va al log y NO a la respuesta HTTP: ponerlo ahi seria
            // regalarle la cuenta a cualquiera que sepa un numero de documento.
            log.warn("[correo] SIN SMTP configurado. Codigo para {}: {}", destinatario, codigo);
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
            log.error("[correo] fallo el envio del codigo de recuperacion", fallo);
        }
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
