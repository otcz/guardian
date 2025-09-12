package otcz.guardian.DTO;

public class MensajeResponse {
    private String mensaje;

    // Constantes de mensajes genéricos reutilizables
    public static final MensajeResponse CREDENCIALES_INVALIDAS = new MensajeResponse("Credenciales inválidas");
    public static final MensajeResponse ACCESO_DENEGADO = new MensajeResponse("Acceso denegado");
    public static final MensajeResponse RECURSO_NO_ENCONTRADO = new MensajeResponse("Recurso no encontrado");
    public static final String MENSAJE_GENERICO = "Ocurrió un error inesperado. Por favor, intente nuevamente.";

    public MensajeResponse(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
