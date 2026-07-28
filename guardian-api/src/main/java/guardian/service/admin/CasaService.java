package guardian.service.admin;

import guardian.dto.admin.CasaRequest;
import guardian.dto.admin.CasaResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

public interface CasaService {

    List<CasaResponse> listar(Long conjuntoId);

    CasaResponse crear(CasaRequest request, UsuarioAutenticado ejecutor);

    CasaResponse actualizar(Long id, CasaRequest request, UsuarioAutenticado ejecutor);

    /**
     * Habilita o deshabilita la casa completa. Deshabilitarla deniega el ingreso
     * de todos sus residentes de una vez, sin tener que tocarlos uno por uno.
     */
    CasaResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor);
}
