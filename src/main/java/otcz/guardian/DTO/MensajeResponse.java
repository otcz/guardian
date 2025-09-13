package otcz.guardian.DTO;

public class MensajeResponse {
    private String mensaje;

    // Constantes de mensajes genéricos reutilizables
    public static final MensajeResponse CREDENCIALES_INVALIDAS = new MensajeResponse("Credenciales inválidas");
    public static final MensajeResponse ACCESO_DENEGADO = new MensajeResponse("Acceso denegado");
    public static final MensajeResponse RECURSO_NO_ENCONTRADO = new MensajeResponse("Recurso no encontrado");
    public static final String MENSAJE_GENERICO = "Ocurrió un error inesperado. Por favor, intente nuevamente.";
    public static final MensajeResponse FALTAN_CAMPOS_OBLIGATORIOS = new MensajeResponse("Faltan campos obligatorios");
    public static final MensajeResponse CORREO_YA_EXISTE = new MensajeResponse("El correo ya existe");
    public static final MensajeResponse DOCUMENTO_YA_EXISTE = new MensajeResponse("La identificación ya existe");
    public static final MensajeResponse USUARIO_NO_ENCONTRADO = new MensajeResponse("Usuario no encontrado");

    // Mensajes de error específicos para campos obligatorios
    public static final String ERROR_CORREO_OBLIGATORIO = "El correo es obligatorio.";
    public static final String ERROR_PASSWORD_OBLIGATORIO = "La contraseña es obligatoria.";
    public static final String ERROR_NOMBRE_OBLIGATORIO = "El nombre completo es obligatorio.";
    public static final String ERROR_TIPO_DOCUMENTO_OBLIGATORIO = "El tipo de documento es obligatorio.";
    public static final String ERROR_NUMERO_DOCUMENTO_OBLIGATORIO = "El número de documento es obligatorio.";
    public static final String ERROR_ROL_OBLIGATORIO = "El rol es obligatorio.";
    public static final String ERROR_ESTADO_OBLIGATORIO = "El estado es obligatorio.";

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
