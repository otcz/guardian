package guardian.service.admin;

import guardian.dto.admin.VehiculoRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

public interface VehiculoService {

    List<VehiculoResponse> listar(Long conjuntoId);

    List<VehiculoResponse> listarPorCasa(Long casaId, Long conjuntoId);

    VehiculoResponse crear(VehiculoRequest request, UsuarioAutenticado ejecutor);

    VehiculoResponse actualizar(Long id, VehiculoRequest request, UsuarioAutenticado ejecutor);

    VehiculoResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor);
}
