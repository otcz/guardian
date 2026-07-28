package guardian.constant;

/**
 * Rutas REST del API. Centralizadas para que un cambio de path sea un cambio de
 * una linea y no una caceria por los controllers.
 */
public final class ApiEndpoint {

    private ApiEndpoint() {
    }

    public static final String API = "/api";

    /** Autenticacion — publico salvo donde se indique. */
    public static final String AUTH = API + "/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_CAMBIAR_CLAVE = "/cambiar-clave";
    public static final String AUTH_YO = "/yo";

    /** Operacion de la garita — rol GUARDIA. */
    public static final String ACCESO = API + "/acceso";
    public static final String ACCESO_VERIFICAR = "/verificar";
    public static final String ACCESO_REGISTRAR = "/registrar";
    public static final String ACCESO_EVENTOS = "/eventos";

    /** Autogestion del residente — rol RESIDENTE. */
    public static final String RESIDENTE = API + "/residente";
    public static final String RESIDENTE_MI_QR = "/mi-qr";
    public static final String RESIDENTE_MI_QR_PNG = "/mi-qr.png";
    public static final String RESIDENTE_MI_CASA = "/mi-casa";

    /** Administracion del conjunto — rol ADMIN. */
    public static final String ADMIN = API + "/admin";
    public static final String ADMIN_CASAS = ADMIN + "/casas";
    public static final String ADMIN_PERSONAS = ADMIN + "/personas";
    public static final String ADMIN_VEHICULOS = ADMIN + "/vehiculos";
    public static final String ADMIN_USUARIOS = ADMIN + "/usuarios";
    public static final String ADMIN_PARAMETROS = ADMIN + "/parametros";

    /** Sufijos reutilizados. */
    public static final String POR_ID = "/{id}";
    public static final String ACTIVAR = "/{id}/activar";
    public static final String DESACTIVAR = "/{id}/desactivar";
    public static final String CREDENCIAL = "/{id}/credencial";
    public static final String CREDENCIAL_PNG = "/{id}/credencial.png";
}
