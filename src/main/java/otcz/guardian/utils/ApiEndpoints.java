package otcz.guardian.utils;

/**
 * Clase utilitaria que centraliza los endpoints de la API REST.
 * Agrupa las rutas por entidad para facilitar su uso y mantenimiento.
 */
public final class ApiEndpoints {

    // Constructor privado para evitar instanciación
    private ApiEndpoints() {}

    /** Endpoints para gestión de usuarios */
    public static final class Usuario {
        private Usuario() {}
        public static final String BASE = "/api/usuarios";
        public static final String POR_ID =  "/{id}";
        public static final String POR_CORREO =  "/correo/{correo}";
        public static final String POR_ROL =  "/rol/{rol}";
        public static final String POR_ESTADO =  "/estado/{estado}";
        public static final String POR_DOCUMENTO = "/documento/{documentoNumero}";
    }

    /** Endpoints para gestión de vehículos */
    public static final class Vehiculo {
        private Vehiculo() {}
        public static final String BASE = "/api/vehiculos";
        public static final String POR_ID =  "/{id}";
        public static final String POR_PLACA = "/{placa}";
        public static final String POR_USUARIO = "/usuario/{usuarioId}";
        public static final String POR_TIPO =  "/tipo/{tipo}";
        public static final String ACTIVOS_POR_USUARIO =  "/usuario/{usuarioId}/activos";
    }

    /** Endpoints para gestión de invitados */
    public static final class Invitado {
        private Invitado() {}
        public static final String BASE = "/api/invitados";
        public static final String POR_ID = BASE + "/{id}";
        public static final String POR_QR = BASE + "/qr/{codigoQR}";
        public static final String POR_USUARIO = BASE + "/usuario/{usuarioId}";
        public static final String ACTIVOS_POR_USUARIO = BASE + "/usuario/{usuarioId}/activos";
        public static final String VALIDOS = BASE + "/validos";
    }

    /** Endpoints para registros de acceso */
    public static final class RegistroAcceso {
        private RegistroAcceso() {}
        public static final String BASE = "/api/registros";
        public static final String POR_USUARIO =  "/usuario/{usuarioId}";
        public static final String POR_INVITADO =  "/invitado/{invitadoId}";
        public static final String POR_VEHICULO = "/vehiculo/{vehiculoId}";
        public static final String POR_RESULTADO =  "/resultado/{resultado}";
        public static final String ENTRE_FECHAS =  "/fechas";
    }

    /** Endpoints para autenticación y perfil */
    public static final class Auth {
        private Auth() {}
        public static final String BASE = "/api/auth";
        public static final String LOGIN = "/login";
        public static final String CAMBIO_PASSWORD = "/cambio-password";
        public static final String PERFIL = "/perfil";
    }

    /** Endpoints para gestión de QR */
    public static final class Qr {
        private Qr() {}
        public static final String BASE = "/api/qr";
        public static final String GENERAR = "/generar";
        public static final String VALIDAR = "/validar";
    }
}
