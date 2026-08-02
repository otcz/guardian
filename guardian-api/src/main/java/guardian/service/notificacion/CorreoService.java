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

    /** false cuando no hay SMTP configurado: el codigo se escribe en el log. */
    boolean estaConfigurado();
}
