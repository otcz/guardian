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
    public static final String ROL_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_GUARDIA = "GUARDIA";
    public static final String ROL_RESIDENTE = "RESIDENTE";

    /**
     * Pseudo-rol de seguridad, NO es un rol del catalogo: lo recibe quien aun
     * debe cambiar su clave inicial y solo le abre los endpoints de
     * autenticacion. Vive aca porque el codigo ramifica sobre el.
     */
    public static final String ROL_CLAVE_PENDIENTE = "CLAVE_PENDIENTE";

    /**
     * Longitud minima de una clave elegida por una persona.
     *
     * <p>Fuente unica: la usan las validaciones de los DTO y el bootstrap para
     * decidir si la clave sembrada es demasiado debil para dejar entrar sin
     * cambiarla. Duplicada como literal, el dia que suba se olvidaria en uno de
     * los tres sitios.</p>
     */
    public static final int CLAVE_LONGITUD_MINIMA = 8;

    /** Tope de BCrypt: ignora en silencio todo byte despues del 72. */
    public static final int CLAVE_LONGITUD_MAXIMA = 72;

    /**
     * Clave con la que nace toda cuenta creada desde un panel.
     *
     * <p>Antes era el documento de la persona. Cuatro ceros es mejor por dos
     * razones: el administrador puede decirla de memoria al entregar el acceso
     * —no tiene que buscar la cedula de cada quien— y se lee como lo que es,
     * un valor de paso, mientras que una clave que ES tu documento invita a
     * dejarla puesta.</p>
     *
     * <p>Que sea trivial de adivinar no la vuelve un riesgo: la cuenta nace
     * INACTIVA y no sirve hasta que el administrador la habilita, y el cambio
     * es obligatorio en el primer ingreso. Ademas es mas corta que
     * {@link #CLAVE_LONGITUD_MINIMA}, asi que nadie puede volver a elegirla.</p>
     */
    public static final String CLAVE_INICIAL = "0000";

    /** Tipos de credencial QR. */
    public static final String CREDENCIAL_PERMANENTE = "PERMANENTE";
    public static final String CREDENCIAL_TEMPORAL = "TEMPORAL";

    /** Grupos del catalogo parametrico. */
    public static final String GRUPO_ROL = "ROL";
    public static final String GRUPO_PARENTESCO = "PARENTESCO";
    public static final String GRUPO_TIPO_VEHICULO = "TIPO_VEHICULO";
    public static final String GRUPO_MOTIVO_DENEGACION = "MOTIVO_DENEGACION";
    public static final String GRUPO_TIPO_CREDENCIAL = "TIPO_CREDENCIAL";
    public static final String GRUPO_TIPO_DOCUMENTO = "TIPO_DOCUMENTO";

    /** Tipo de documento por defecto cuando el alta no lo especifica. */
    public static final String TIPO_DOCUMENTO_CC = "CC";

    /** Motivos de denegacion. Sembrados en GD_PARAMETRO, referenciados aca. */
    public static final String MOTIVO_FIRMA_INVALIDA = "FIRMA_INVALIDA";
    public static final String MOTIVO_CREDENCIAL_REVOCADA = "CREDENCIAL_REVOCADA";
    public static final String MOTIVO_CREDENCIAL_VENCIDA = "CREDENCIAL_VENCIDA";
    public static final String MOTIVO_PERSONA_INACTIVA = "PERSONA_INACTIVA";
    public static final String MOTIVO_CASA_INACTIVA = "CASA_INACTIVA";
    public static final String MOTIVO_INVITACION_NO_VIGENTE = "INVITACION_NO_VIGENTE";
    public static final String MOTIVO_INVITACION_AGOTADA = "INVITACION_AGOTADA";

    /**
     * Bloqueos de la administracion. Se distinguen de PERSONA_INACTIVA y
     * CASA_INACTIVA a proposito: al guardia y al residente hay que decirles
     * cosas distintas segun quien apago el interruptor.
     */
    public static final String MOTIVO_PERSONA_BLOQUEADA = "PERSONA_BLOQUEADA";
    public static final String MOTIVO_CASA_BLOQUEADA = "CASA_BLOQUEADA";
    public static final String MOTIVO_VEHICULO_BLOQUEADO = "VEHICULO_BLOQUEADO";

    /** Parentescos con significado especial en la logica. */
    public static final String PARENTESCO_TITULAR = "TITULAR";
}
