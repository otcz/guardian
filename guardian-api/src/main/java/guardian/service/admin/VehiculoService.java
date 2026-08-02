package guardian.service.admin;

import guardian.dto.admin.VehiculoRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

public interface VehiculoService {

    List<VehiculoResponse> listar(Long conjuntoId);

    /** Solo activos: es lo que consume la porteria para los botones de placa. */
    List<VehiculoResponse> listarPorCasa(Long casaId, Long conjuntoId);

    /** Todos, activos e inactivos: el residente necesita ver el inactivo para reactivarlo. */
    List<VehiculoResponse> listarPorCasaIncluyendoInactivos(Long casaId, Long conjuntoId);

    VehiculoResponse crear(VehiculoRequest request, UsuarioAutenticado ejecutor);

    VehiculoResponse actualizar(Long id, VehiculoRequest request, UsuarioAutenticado ejecutor);

    VehiculoResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor);

    /**
     * Eliminacion FISICA — exclusiva del super administrador. El residente y
     * el administrador de la sede solo inactivan, que deja rastro.
     *
     * <p>La bitacora conserva la placa copiada en cada evento.</p>
     */
    void eliminar(Long id, UsuarioAutenticado ejecutor);
}
