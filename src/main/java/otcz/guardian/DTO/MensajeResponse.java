package otcz.guardian.DTO;

public class MensajeResponse {
    private String mensaje;

    // Constantes de mensajes genéricos reutilizables
    public static final MensajeResponse CREDENCIALES_INVALIDAS = new MensajeResponse("CREDENCIALES INVÁLIDAS");
    public static final MensajeResponse ACCESO_DENEGADO = new MensajeResponse("ACCESO DENEGADO");
    public static final MensajeResponse RECURSO_NO_ENCONTRADO = new MensajeResponse("RECURSO NO ENCONTRADO");
    public static final String MENSAJE_GENERICO = "OCURRIÓ UN ERROR INESPERADO. POR FAVOR, INTENTE NUEVAMENTE.";
    public static final MensajeResponse FALTAN_CAMPOS_OBLIGATORIOS = new MensajeResponse("FALTAN CAMPOS OBLIGATORIOS");
    public static final MensajeResponse CORREO_YA_EXISTE = new MensajeResponse("EL CORREO YA EXISTE");
    public static final MensajeResponse DOCUMENTO_YA_EXISTE = new MensajeResponse("LA IDENTIFICACIÓN YA EXISTE");
    public static final MensajeResponse USUARIO_NO_ENCONTRADO = new MensajeResponse("USUARIO NO ENCONTRADO");
    public static final MensajeResponse USUARIO_ELIMINADO = new MensajeResponse("USUARIO ELIMINADO CORRECTAMENTE");

    // Mensajes de error específicos para campos obligatorios
    public static final String ERROR_CORREO_OBLIGATORIO = "EL CORREO ES OBLIGATORIO.";
    public static final String ERROR_PASSWORD_OBLIGATORIO = "LA CONTRASEÑA ES OBLIGATORIA.";
    public static final String ERROR_NOMBRE_OBLIGATORIO = "EL NOMBRE COMPLETO ES OBLIGATORIO.";
    public static final String ERROR_TIPO_DOCUMENTO_OBLIGATORIO = "EL TIPO DE DOCUMENTO ES OBLIGATORIO.";
    public static final String ERROR_NUMERO_DOCUMENTO_OBLIGATORIO = "EL NÚMERO DE DOCUMENTO ES OBLIGATORIO.";
    public static final String ERROR_ROL_OBLIGATORIO = "EL ROL ES OBLIGATORIO.";
    public static final String ERROR_ESTADO_OBLIGATORIO = "EL ESTADO ES OBLIGATORIO.";

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
