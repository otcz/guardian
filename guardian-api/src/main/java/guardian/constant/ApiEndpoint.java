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
    /** Las porterias que la tablet puede elegir, y cual le toca a quien entro. */
    public static final String ACCESO_PORTERIAS = "/porterias";

    /** Autogestion del residente — cualquier usuario autenticado sobre SU casa. */
    public static final String RESIDENTE = API + "/residente";
    public static final String RESIDENTE_MI_QR = "/mi-qr";
    public static final String RESIDENTE_MI_QR_PNG = "/mi-qr.png";
    public static final String RESIDENTE_MI_FOTO = "/mi-foto";
    public static final String RESIDENTE_FAMILIA = "/familia";
    public static final String RESIDENTE_VEHICULOS = "/vehiculos";
    public static final String RESIDENTE_INVITACIONES = "/invitaciones";
    /** Codigo con el que un familiar se registra dentro de este hogar. */
    public static final String RESIDENTE_CODIGO_HOGAR = "/hogar/codigo";

    /** Fotos: subida autenticada, lectura publica por nombre UUID. */
    public static final String FOTOS = API + "/fotos";
    public static final String PUBLICO_FOTOS = API + "/publico/fotos";

    /**
     * Catalogos para CUALQUIER usuario autenticado. Los formularios del
     * residente (parentescos, tipos de vehiculo) los necesitan igual que el
     * back-office; servirlos solo bajo /admin les daba 403 a los residentes.
     */
    public static final String PARAMETROS = API + "/parametros";

    /** Pagina del invitado: abre el link sin cuenta, protegido por codigo UUID. */
    public static final String PUBLICO_INVITACIONES = API + "/publico/invitaciones";

    /** Registro dentro de un hogar existente: sin cuenta, el codigo es la llave. */
    public static final String PUBLICO_HOGAR = API + "/publico/hogar";

    /**
     * Recuperacion de clave. Va bajo /publico y no bajo /auth porque quien la
     * usa NO tiene sesion —ese es el problema que viene a resolver— y /auth
     * exige estar autenticado.
     */
    public static final String PUBLICO_RECUPERACION = API + "/publico/recuperacion";

    /** Administracion del conjunto — rol ADMIN. */
    public static final String ADMIN = API + "/admin";
    public static final String ADMIN_RESUMEN = ADMIN + "/resumen";
    public static final String ADMIN_CASAS = ADMIN + "/casas";
    /** Puntos de acceso del conjunto. En la pantalla se llaman porterias. */
    public static final String ADMIN_PORTERIAS = ADMIN + "/porterias";
    public static final String ADMIN_PERSONAS = ADMIN + "/personas";
    public static final String ADMIN_VEHICULOS = ADMIN + "/vehiculos";
    public static final String ADMIN_USUARIOS = ADMIN + "/usuarios";
    public static final String ADMIN_INVITACIONES = ADMIN + "/invitaciones";
    public static final String ADMIN_PARAMETROS = ADMIN + "/parametros";

    /** Bloqueos administrativos: la llave que el residente no puede levantar. */
    public static final String ADMIN_BLOQUEOS = ADMIN + "/bloqueos";

    /**
     * Plataforma — rol SUPER_ADMIN. Prefijo PROPIO y no anidado bajo /admin:
     * si colgara de ahi, el matcher de /api/admin/** ganaria por orden y el
     * super administrador recibiria 403 en sus propios endpoints.
     */
    public static final String SUPER = API + "/super";
    public static final String SUPER_SEDES = SUPER + "/sedes";
    public static final String SEDE_ADMINISTRADOR = "/{id}/administrador";
    public static final String SEDE_ENTRAR = "/{id}/entrar";
    public static final String SEDE_SALIR = "/salir";

    /** Sufijos reutilizados. */
    public static final String POR_ID = "/{id}";
    public static final String ACTIVAR = "/{id}/activar";
    public static final String DESACTIVAR = "/{id}/desactivar";
    public static final String GUARDIAS = "/{id}/guardias";
    public static final String CREDENCIAL = "/{id}/credencial";
    public static final String CREDENCIAL_PNG = "/{id}/credencial.png";
}
