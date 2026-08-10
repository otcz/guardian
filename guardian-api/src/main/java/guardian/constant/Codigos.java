package guardian.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    /**
     * Estados de una solicitud de casa. En Codigos y no en GD_PARAMETRO porque
     * el codigo RAMIFICA sobre ellos —solo una PENDIENTE se puede aprobar— y un
     * administrador borrandolos desde Configuracion dejaria la bandeja muerta.
     */
    public static final String SOLICITUD_PENDIENTE = "PENDIENTE";
    public static final String SOLICITUD_APROBADA = "APROBADA";
    public static final String SOLICITUD_RECHAZADA = "RECHAZADA";

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
     * El acceso es un PIN de CUATRO digitos, no una contrasena.
     *
     * <p>Decision de producto: esto lo usa gente de todas las edades desde el
     * telefono, y un PIN es el gesto que ya conocen de desbloquear el celular
     * y del cajero. Una contrasena de ocho caracteres en un teclado tactil, de
     * pie en la porteria, produce el resultado contrario al que busca: se
     * anota en un papel pegado a la puerta.</p>
     *
     * <p><b>El costo esta medido.</b> Cuatro digitos son diez mil
     * combinaciones. Con el bloqueo de {@code login-max-intentos} agotarlas
     * toma semanas, no minutos — pero sigue siendo un margen estrecho para una
     * cuenta de administrador. Se compensa rechazando los PIN triviales, que
     * es donde vive el riesgo real: nadie ataca esto probando 6 mil numeros al
     * azar, prueba 0000, 1234 y la fecha de nacimiento. Ver
     * {@link guardian.util.PinUtil}.</p>
     *
     * <p>Fuente unica: la usan los DTO, el bootstrap y las pantallas. Duplicada
     * como literal, el dia que cambie se olvidaria en uno de los sitios.</p>
     */
    public static final int PIN_LONGITUD = 4;

    /**
     * PIN con el que nace toda cuenta creada desde un panel.
     *
     * <p>Antes era el documento de la persona. Cuatro ceros es mejor por dos
     * razones: el administrador puede decirlo de memoria al entregar el acceso
     * —no tiene que buscar la cedula de cada quien— y se lee como lo que es,
     * un valor de paso, mientras que un PIN que ES tu documento invita a
     * dejarlo puesto.</p>
     *
     * <p>Que sea trivial no lo vuelve un riesgo: el cambio es obligatorio en el
     * primer ingreso, y {@link guardian.util.PinUtil} lo rechaza como PIN
     * elegido — asi que nadie puede "cambiarlo" dejandolo igual.</p>
     */
    public static final String CLAVE_INICIAL = "0000";

    /** Tipos de credencial QR. */
    public static final String CREDENCIAL_PERMANENTE = "PERMANENTE";
    public static final String CREDENCIAL_TEMPORAL = "TEMPORAL";

    /** Grupos del catalogo parametrico. */
    public static final String GRUPO_ROL = "ROL";
    public static final String GRUPO_PARENTESCO = "PARENTESCO";
    public static final String GRUPO_TIPO_VEHICULO = "TIPO_VEHICULO";
    public static final String GRUPO_MARCA_VEHICULO = "MARCA_VEHICULO";
    public static final String GRUPO_COLOR_VEHICULO = "COLOR_VEHICULO";
    public static final String GRUPO_MOTIVO_DENEGACION = "MOTIVO_DENEGACION";
    public static final String GRUPO_TIPO_CREDENCIAL = "TIPO_CREDENCIAL";
    public static final String GRUPO_TIPO_DOCUMENTO = "TIPO_DOCUMENTO";
    public static final String GRUPO_TIPO_VIVIENDA = "TIPO_VIVIENDA";

    /**
     * Los grupos que el administrador puede ampliar desde el panel.
     *
     * <p>Los que NO estan aca —ROL, MOTIVO_DENEGACION, TIPO_CREDENCIAL— son
     * estructurales: el codigo ramifica sobre sus codigos exactos. Cambiarles
     * la ETIQUETA es inofensivo y se permite; agregar o quitar opciones romperia
     * validaciones que no tienen como enterarse.</p>
     */
    public static final List<String> GRUPOS_ABIERTOS = Collections
            .unmodifiableList(Arrays.asList(
                    GRUPO_PARENTESCO,
                    GRUPO_TIPO_DOCUMENTO,
                    GRUPO_TIPO_VIVIENDA,
                    GRUPO_TIPO_VEHICULO,
                    GRUPO_MARCA_VEHICULO,
                    GRUPO_COLOR_VEHICULO));

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
    public static final String MOTIVO_INVITACION_PENDIENTE_APROBACION =
            "INVITACION_PENDIENTE_APROBACION";

    /**
     * Bloqueos de la administracion. Se distinguen de PERSONA_INACTIVA y
     * CASA_INACTIVA a proposito: al guardia y al residente hay que decirles
     * cosas distintas segun quien apago el interruptor.
     */
    public static final String MOTIVO_PERSONA_BLOQUEADA = "PERSONA_BLOQUEADA";
    public static final String MOTIVO_CASA_BLOQUEADA = "CASA_BLOQUEADA";
    public static final String MOTIVO_VEHICULO_BLOQUEADO = "VEHICULO_BLOQUEADO";

    /**
     * Negaciones que antes solo lanzaban un error y no dejaban rastro.
     *
     * <p>Un vehiculo que la administracion deshabilito, alguien mandando el id
     * de un carro de otra casa, un invitado que llega en carro sin placa
     * declarada: todo eso pasaba por la porteria y la bitacora quedaba en
     * blanco. Justamente lo que hay que poder mirar despues.</p>
     */
    public static final String MOTIVO_VEHICULO_INACTIVO = "VEHICULO_INACTIVO";
    public static final String MOTIVO_VEHICULO_AJENO = "VEHICULO_AJENO";
    public static final String MOTIVO_INVITADO_SIN_VEHICULO = "INVITADO_SIN_VEHICULO";
    public static final String MOTIVO_ENTRADA_TRAS_SALIDA = "ENTRADA_TRAS_SALIDA";

    /** Parentescos con significado especial en la logica. */
    public static final String PARENTESCO_TITULAR = "TITULAR";
}
