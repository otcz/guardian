package guardian.service.admin;

import guardian.dto.common.ParametroResponse;

import java.util.List;

public interface ParametroService {

    List<ParametroResponse> listarPorGrupo(String grupo);

    /**
     * Valida que un codigo exista y este activo dentro de su grupo.
     *
     * @throws guardian.exception.GuardianException 400 si no existe. Es la
     *         puerta que impide que llegue a la base un parentesco o un tipo de
     *         vehiculo inventado por el cliente.
     */
    void exigirCodigoValido(String grupo, String codigo);
}
