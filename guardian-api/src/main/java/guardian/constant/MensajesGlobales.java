package guardian.constant;

/**
 * Mensajes que llegan al usuario final. Centralizados para mantener un tono
 * consistente y en espanol neutro (ver .claude/CLAUDE.md seccion 6).
 */
public final class MensajesGlobales {

    private MensajesGlobales() {
    }

    // ── Autenticacion ────────────────────────────────────────────────────────
    public static final String CREDENCIALES_INVALIDAS =
            "El documento o la contrasena no son correctos.";
    public static final String USUARIO_INACTIVO =
            "Tu usuario esta inactivo. Comunicate con la administracion del conjunto.";
    public static final String DEBE_CAMBIAR_CLAVE =
            "Debes cambiar tu contrasena antes de continuar.";
    public static final String CLAVE_ACTUAL_INCORRECTA =
            "La contrasena actual no es correcta.";
    public static final String CLAVE_IGUAL_AL_DOCUMENTO =
            "La nueva contrasena no puede ser tu numero de documento.";
    public static final String CLAVE_MUY_CORTA =
            "La contrasena debe tener al menos 8 caracteres.";
    public static final String SESION_REQUERIDA =
            "Necesitas iniciar sesion para continuar.";
    public static final String SIN_PERMISO =
            "No tienes permiso para realizar esta accion.";

    // ── Acceso ───────────────────────────────────────────────────────────────
    public static final String QR_NO_RECONOCIDO =
            "El codigo no es valido.";
    public static final String QR_REVOCADO =
            "Esta credencial fue revocada.";
    public static final String QR_VENCIDO =
            "Esta credencial ya vencio.";
    public static final String PERSONA_INACTIVA =
            "Esta persona no tiene el ingreso habilitado.";
    public static final String CASA_INACTIVA =
            "La casa no tiene el ingreso habilitado.";
    public static final String VEHICULO_NO_PERTENECE =
            "El vehiculo seleccionado no esta registrado para esa casa.";
    public static final String ACCESO_PERMITIDO =
            "Ingreso registrado.";

    // ── Administracion ───────────────────────────────────────────────────────
    public static final String DOCUMENTO_YA_REGISTRADO =
            "Ya existe una persona con ese numero de documento.";
    public static final String PLACA_YA_REGISTRADA =
            "Ya existe un vehiculo con esa placa.";
    public static final String CASA_YA_REGISTRADA =
            "Ya existe una casa con ese identificador.";
    public static final String PERSONA_SIN_FOTO =
            "La persona necesita una foto antes de que se le emita la credencial.";
    public static final String TITULAR_YA_EXISTE =
            "Esa casa ya tiene un titular asignado.";

    // ── Genericos ────────────────────────────────────────────────────────────
    public static final String NO_ENCONTRADO = "No encontramos lo que buscabas.";
    public static final String ERROR_INESPERADO =
            "Ocurrio un error inesperado. Intenta de nuevo en unos minutos.";
}
