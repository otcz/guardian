package guardian.constant;

/**
 * Codigos estructurales del dominio.
 *
 * <p><b>Que hace un codigo aca y no en el catalogo GD_PARAMETRO.</b> Estos son
 * discriminadores sobre los que el codigo ramifica: agregar un valor nuevo
 * exigiria escribir logica nueva de todas formas. Ponerlos en el catalogo daria
 * la ilusion de que el administrador puede inventar un sentido de acceso nuevo,
 * y no puede.</p>
 *
 * <p>Todo lo que el administrador SI puede cambiar sin deploy — parentescos,
 * tipos de vehiculo, motivos de denegacion — vive en GD_PARAMETRO. Ver
 * .claude/CONTEXT.md seccion 6.</p>
 */
public final class Codigos {

    private Codigos() {
    }

    /** Valores del flag {@code activo} heredado de BaseEntity. */
    public static final String SI = "S";
    public static final String NO = "N";

    /** Sentido del evento de acceso. */
    public static final String ENTRADA = "E";
    public static final String SALIDA = "S";

    /** Modo en que la persona cruza la porteria. */
    public static final String MODO_PEATON = "PEATON";
    public static final String MODO_VEHICULO = "VEHICULO";

    /** Resultado del intento de acceso. */
    public static final String RESULTADO_PERMITIDO = "PERMITIDO";
    public static final String RESULTADO_DENEGADO = "DENEGADO";

    /** Roles de usuario. */
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_GUARDIA = "GUARDIA";
    public static final String ROL_RESIDENTE = "RESIDENTE";

    /**
     * Pseudo-rol de seguridad, NO es un rol del catalogo: lo recibe quien aun
     * debe cambiar su clave inicial y solo le abre los endpoints de
     * autenticacion. Vive aca porque el codigo ramifica sobre el.
     */
    public static final String ROL_CLAVE_PENDIENTE = "CLAVE_PENDIENTE";

    /** Tipos de credencial QR. */
    public static final String CREDENCIAL_PERMANENTE = "PERMANENTE";
    public static final String CREDENCIAL_TEMPORAL = "TEMPORAL";

    /** Grupos del catalogo parametrico. */
    public static final String GRUPO_ROL = "ROL";
    public static final String GRUPO_PARENTESCO = "PARENTESCO";
    public static final String GRUPO_TIPO_VEHICULO = "TIPO_VEHICULO";
    public static final String GRUPO_MOTIVO_DENEGACION = "MOTIVO_DENEGACION";
    public static final String GRUPO_TIPO_CREDENCIAL = "TIPO_CREDENCIAL";

    /** Motivos de denegacion. Sembrados en GD_PARAMETRO, referenciados aca. */
    public static final String MOTIVO_FIRMA_INVALIDA = "FIRMA_INVALIDA";
    public static final String MOTIVO_CREDENCIAL_REVOCADA = "CREDENCIAL_REVOCADA";
    public static final String MOTIVO_CREDENCIAL_VENCIDA = "CREDENCIAL_VENCIDA";
    public static final String MOTIVO_PERSONA_INACTIVA = "PERSONA_INACTIVA";
    public static final String MOTIVO_CASA_INACTIVA = "CASA_INACTIVA";
    public static final String MOTIVO_INVITACION_NO_VIGENTE = "INVITACION_NO_VIGENTE";
    public static final String MOTIVO_INVITACION_AGOTADA = "INVITACION_AGOTADA";

    /** Parentescos con significado especial en la logica. */
    public static final String PARENTESCO_TITULAR = "TITULAR";
}
