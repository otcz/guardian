package otcz.guardian.utils;

public final class ApiEndpoints {

    private ApiEndpoints() {
        // Constructor privado para evitar instanciación
    }

    public static final class Usuario {
        public static final String BASE = "/api/usuarios";
        public static final String POR_ID = "/{id}";
        public static final String POR_CORREO = "/correo/{correo}";
        public static final String POR_ROL = "/rol/{rol}";
        public static final String POR_ESTADO = "/estado/{estado}";
    }

    public static final class Vehiculo {
        public static final String BASE = "/api/vehiculos";
        public static final String POR_ID = "/{id}";
        public static final String POR_PLACA = "/{placa}";
        public static final String POR_USUARIO = "/usuario/{usuarioId}";
        public static final String POR_TIPO = "/tipo/{tipo}";
        public static final String ACTIVOS_POR_USUARIO = "/usuario/{usuarioId}/activos";
    }

    public static final class Invitado {
        public static final String BASE = "/api/invitados";
        public static final String POR_ID = "/{id}";
        public static final String POR_QR = "/qr/{codigoQR}";
        public static final String POR_USUARIO = "/usuario/{usuarioId}";
        public static final String ACTIVOS_POR_USUARIO = "/usuario/{usuarioId}/activos";
        public static final String VALIDOS = "/validos";
    }

    public static final class RegistroAcceso {
        public static final String BASE = "/api/registros";
        public static final String POR_USUARIO = "/usuario/{usuarioId}";
        public static final String POR_INVITADO = "/invitado/{invitadoId}";
        public static final String POR_VEHICULO = "/vehiculo/{vehiculoId}";
        public static final String POR_RESULTADO = "/resultado/{resultado}";
        public static final String ENTRE_FECHAS = "/fechas";
    }

    public static final class Auth {
        public static final String LOGIN = "/api/auth/login";
        public static final String CAMBIO_PASSWORD = "/api/auth/cambio-password";
        public static final String PERFIL = "/api/auth/perfil";
    }
}
