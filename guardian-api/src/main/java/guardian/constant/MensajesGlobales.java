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
            "El documento o el PIN no son correctos.";
    /**
     * Dice "cuenta" y no "usuario" a proposito. En el panel de Personas el
     * registro de alguien puede figurar como "Activa" mientras su CUENTA sigue
     * sin habilitar — son dos cosas distintas — y el mensaje anterior parecia
     * contradecir a la pantalla.
     */
    public static final String USUARIO_INACTIVO =
            "Tu cuenta todavia no esta habilitada. "
            + "Comunicate con la administracion del conjunto.";
    public static final String DEBE_CAMBIAR_CLAVE =
            "Debes cambiar tu PIN antes de continuar.";
    public static final String CLAVE_ACTUAL_INCORRECTA =
            "El PIN actual no es correcto.";
    public static final String PIN_FORMA_INVALIDA =
            "El PIN son 4 numeros.";
    /**
     * Se dice QUE hacer y no solo que esta mal. "PIN invalido" a secas manda a
     * la persona a probar otro trivial hasta que acierte uno permitido.
     */
    public static final String PIN_TRIVIAL =
            "Ese PIN es muy facil de adivinar. Evita repetidos (1111), "
            + "seguidos (1234) y anos.";
    public static final String PIN_SALE_DEL_DOCUMENTO =
            "El PIN no puede salir de tu documento: cualquiera que lo haya "
            + "visto lo sabria.";
    /**
     * UN SOLO mensaje para codigo equivocado, vencido, ya usado, agotado y
     * documento inexistente. Distinguirlos le diria a quien prueba codigos
     * cual de las cinco cosas acerto.
     */
    public static final String CODIGO_RECUPERACION_NO_VALIDO =
            "Ese codigo no sirve. Pide uno nuevo.";
    /** Identico exista o no la cuenta: ver SolicitudCodigoResponse. */
    public static final String CODIGO_RECUPERACION_ENVIADO =
            "Si ese documento tiene una cuenta con correo registrado, "
            + "te enviamos un codigo.";
    public static final String SESION_REQUERIDA =
            "Necesitas iniciar sesion para continuar.";
    public static final String SIN_PERMISO =
            "No tienes permiso para realizar esta accion.";
    public static final String BORRADO_SOLO_SUPER_ADMIN =
            "Solo el super administrador puede eliminar. Puedes desactivar en su lugar.";

    // ── Sedes ────────────────────────────────────────────────────────────────
    public static final String SIN_SEDE_ELEGIDA =
            "Primero entra a una sede para administrarla.";
    public static final String SEDE_NO_ENCONTRADA =
            "No encontramos esa sede.";
    public static final String SEDE_YA_REGISTRADA =
            "Ya existe una sede con ese nombre.";
    public static final String SEDE_YA_TIENE_ADMIN =
            "Esa sede ya tiene un administrador.";
    public static final String SEDE_NO_OPERATIVA =
            "Esa sede esta desactivada. Activala antes de entrar.";
    public static final String ROL_NO_ASIGNABLE =
            "Ese rol no se puede asignar desde aca.";
    public static final String ADMIN_SOLO_SUPER_ADMIN =
            "Solo el super administrador puede nombrar administradores.";

    // ── Acceso ───────────────────────────────────────────────────────────────
    public static final String QR_NO_RECONOCIDO =
            "El codigo no es valido.";
    public static final String QR_REVOCADO =
            "Esta credencial fue revocada.";
    public static final String QR_VENCIDO =
            "Esta credencial ya vencio.";
    // ── Llave del titular: activo ────────────────────────────────────────────
    // Lo que el hogar apago. Se dice "inactiva", nunca "deshabilitada": esa
    // palabra es de la otra llave, y al guardia le tienen que quedar claras
    // las dos causas porque manda a la persona a sitios distintos — al titular
    // de su casa o a la administracion.
    public static final String PERSONA_INACTIVA =
            "Su hogar tiene esta persona inactiva.";
    public static final String CASA_INACTIVA =
            "Esta casa esta inactiva.";
    public static final String VEHICULO_INACTIVO =
            "Este vehiculo esta inactivo. Su hogar puede activarlo.";

    // ── Llave de la administracion: bloqueado ────────────────────────────────
    public static final String PERSONA_BLOQUEADA =
            "La administracion deshabilito el ingreso de esta persona.";
    public static final String CASA_BLOQUEADA =
            "La administracion deshabilito el ingreso de esta casa.";
    public static final String VEHICULO_BLOQUEADO =
            "La administracion deshabilito este vehiculo.";
    public static final String DESBLOQUEO_SOLO_ADMIN =
            "Esto lo deshabilito la administracion. Solo ella puede habilitarlo de nuevo.";
    public static final String BLOQUEO_SOLO_ADMIN =
            "Deshabilitar es una accion de la administracion.";
    public static final String VEHICULO_NO_PERTENECE =
            "El vehiculo seleccionado no esta registrado para esa casa.";
    public static final String ACCESO_PERMITIDO =
            "Ingreso registrado.";
    public static final String SELECCIONA_VEHICULO =
            "Selecciona el vehiculo.";
    public static final String SOLO_SALIDA =
            "Puede salir, pero no volver a entrar.";

    // ── Administracion ───────────────────────────────────────────────────────
    public static final String DOCUMENTO_YA_REGISTRADO =
            "Ya existe una persona con ese numero de documento.";
    public static final String TELEFONO_YA_REGISTRADO =
            "Ese telefono ya esta registrado por otra persona.";
    public static final String CORREO_REQUERIDO_CON_CUENTA =
            "Escribe el correo: es por donde la persona recupera su PIN.";
    public static final String PLACA_YA_REGISTRADA =
            "Ya existe un vehiculo con esa placa.";
    public static final String PERSONA_YA_EN_UNA_CASA =
            "Esa persona ya pertenece a otra casa.";
    public static final String CASA_YA_REGISTRADA =
            "Ya existe una casa con ese identificador.";
    public static final String PORTERIA_YA_REGISTRADA =
            "Ya existe una porteria con ese nombre.";
    /**
     * Sin porterias activas la fabrica de eventos deja el punto de acceso en
     * nulo y la bitacora deja de decir por donde paso la gente. Se corta aca y
     * no en la garita, donde el error aparecerian meses despues.
     */
    public static final String PORTERIA_ULTIMA_ACTIVA =
            "Es la unica porteria activa. Activa otra antes de apagar esta.";
    public static final String PERSONA_SIN_FOTO =
            "La persona necesita una foto antes de que se le emita la credencial.";
    public static final String TITULAR_YA_EXISTE =
            "Esa casa ya tiene un titular asignado.";

    // ── Presencia ────────────────────────────────────────────────────────────
    public static final String YA_ADENTRO =
            "Esta persona ya registro su entrada y aun no ha salido.";
    public static final String YA_AFUERA =
            "Esta persona ya registro su salida.";

    // ── Autogestion del residente ────────────────────────────────────────────
    public static final String SIN_CASA =
            "Tu usuario no tiene una casa asignada. Comunicate con la administracion.";
    public static final String FAMILIAR_AJENO =
            "Esa persona no pertenece a tu casa.";
    public static final String NO_INACTIVARSE_A_SI_MISMO =
            "No puedes inactivarte a ti mismo.";
    public static final String TITULAR_SOLO_ADMIN =
            "El titular de la casa solo lo asigna la administracion.";
    public static final String SOLO_TITULAR_FAMILIA =
            "Solo el titular de la casa administra la familia.";

    // ── Codigo para unirse a un hogar ────────────────────────────────────────
    /**
     * Un mensaje unico para vencido, revocado y ya usado. Quien esta al otro
     * lado no necesita distinguirlos, y a quien prueba codigos no hay por que
     * decirle cual de los tres acerto.
     */
    public static final String CODIGO_HOGAR_NO_VALIDO =
            "Ese codigo ya no sirve. Pidele uno nuevo al titular de la casa.";
    public static final String PARENTESCO_TITULAR_NO =
            "Esa casa ya tiene titular. Elige tu parentesco con el.";

    // ── Invitaciones ─────────────────────────────────────────────────────────
    public static final String INVITACION_VIGENCIA_INVALIDA =
            "La vigencia debe terminar despues de empezar.";
    public static final String INVITACION_EN_PASADO =
            "La visita no puede ser en el pasado.";
    public static final String INVITACION_MUY_LARGA =
            "La vigencia supera el maximo permitido.";
    public static final String INVITACION_NO_VIGENTE_MSG =
            "Esta invitacion aun no esta vigente.";
    public static final String INVITACION_AGOTADA_MSG =
            "Esta invitacion ya uso todos sus ingresos.";
    public static final String INVITADO_SIN_VEHICULO =
            "El invitado no registro vehiculo en la invitacion.";

    // ── Fotos ────────────────────────────────────────────────────────────────
    public static final String FOTO_INVALIDA =
            "La foto debe ser JPG, PNG o WEBP y pesar menos de 5 MB.";
    public static final String FOTO_URL_INVALIDA =
            "La foto debe subirse desde la aplicacion.";

    // ── Credenciales ─────────────────────────────────────────────────────────
    public static final String SIN_CREDENCIAL =
            "Esta persona todavia no tiene credencial.";

    // ── Eliminacion (solo administrador) ─────────────────────────────────────
    public static final String NO_ELIMINARSE_A_SI_MISMO =
            "No puedes eliminar tu propia persona.";

    // ── Genericos ────────────────────────────────────────────────────────────
    public static final String NO_ENCONTRADO = "No encontramos lo que buscabas.";
    public static final String ERROR_INESPERADO =
            "Ocurrio un error inesperado. Intenta de nuevo en unos minutos.";
    public static final String OPERACION_CRUZADA =
            "Otra persona hizo un cambio al mismo tiempo. Intenta de nuevo.";
}
