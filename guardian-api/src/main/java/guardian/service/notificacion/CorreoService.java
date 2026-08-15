package guardian.service.notificacion;

public interface CorreoService {

    /**
     * Envia el codigo de recuperacion.
     *
     * <p>No lanza si el envio falla. Quien lo llama esta respondiendo "si ese
     * documento existe, te llego un codigo" —una respuesta que a proposito no
     * revela nada— y propagar el fallo del SMTP convertiria esa respuesta en
     * un delator: error significaria "existe", exito significaria "no
     * existe".</p>
     */
    void enviarCodigoRecuperacion(String destinatario, String nombre, String codigo,
                                  int minutosVigencia);

    /**
     * Un correo cualquiera, en texto plano.
     *
     * <p><b>Nunca lanza</b>, igual que el envio del codigo, y por una razon mas
     * fuerte todavia: quien llama a esto ya aprobo un vehiculo o bloqueo a una
     * persona. Si un SMTP caido tumbara esa operacion, el sistema quedaria sin
     * poder administrar nada cada vez que Google tenga un mal dia.</p>
     *
     * <p>Sin SMTP configurado escribe el mensaje en el log y sigue.</p>
     */
    void enviar(String destinatario, String asunto, String cuerpo);

    /** false cuando no hay SMTP configurado: el mensaje se escribe en el log. */
    boolean estaConfigurado();
}
