package guardian.service.admin;

import guardian.dto.admin.GrupoParametroResponse;
import guardian.dto.admin.ParametroRequest;
import guardian.dto.common.ParametroResponse;

import java.util.List;

public interface ParametroService {

    /** Solo las opciones activas: lo que alimenta los selects de la aplicacion. */
    List<ParametroResponse> listarPorGrupo(String grupo);

    /**
     * Valida que un codigo exista y este activo dentro de su grupo.
     *
     * @throws guardian.exception.GuardianException 400 si no existe. Es la
     *         puerta que impide que llegue a la base un parentesco o un tipo de
     *         vehiculo inventado por el cliente.
     */
    void exigirCodigoValido(String grupo, String codigo);

    /** Los grupos que ve el administrador en Configuracion. */
    List<GrupoParametroResponse> listarGrupos();

    /**
     * Activas e inactivas. El panel de administracion necesita ver las que
     * apago: si solo viera las activas, apagar una opcion equivaldria a
     * borrarla y no habria forma de volver a encenderla.
     */
    List<ParametroResponse> administrarGrupo(String grupo);

    ParametroResponse crear(String grupo, ParametroRequest solicitud);

    /** Cambia el texto visible. El codigo nunca se toca: hay filas apuntando a el. */
    ParametroResponse renombrar(Long id, ParametroRequest solicitud);

    /**
     * Enciende o apaga una opcion.
     *
     * <p>Apagar no borra: los vehiculos que ya declararon esa marca la
     * conservan, y la opcion deja de ofrecerse en los formularios nuevos. Por
     * eso el catalogo no tiene un borrado real.</p>
     */
    ParametroResponse cambiarEstado(Long id, boolean activo);
}
