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

    // ── Bloqueos de la administracion ────────────────────────────────────────
    public static final String PERSONA_BLOQUEADA =
            "La administracion bloqueo el ingreso de esta persona.";
    public static final String CASA_BLOQUEADA =
            "La administracion bloqueo el ingreso de esta casa.";
    public static final String VEHICULO_BLOQUEADO =
            "La administracion bloqueo este vehiculo.";
    public static final String DESBLOQUEO_SOLO_ADMIN =
            "Esto lo bloqueo la administracion. Solo ella puede habilitarlo de nuevo.";
    public static final String BLOQUEO_SOLO_ADMIN =
            "Bloquear es una accion de la administracion.";
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
    public static final String PLACA_YA_REGISTRADA =
            "Ya existe un vehiculo con esa placa.";
    public static final String PERSONA_YA_EN_UNA_CASA =
            "Esa persona ya pertenece a otra casa.";
    public static final String CASA_YA_REGISTRADA =
            "Ya existe una casa con ese identificador.";
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
