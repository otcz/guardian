package guardian.service.admin;

import guardian.dto.admin.ResumenResponse;
import guardian.security.UsuarioAutenticado;

public interface ResumenService {

    ResumenResponse resumen(UsuarioAutenticado ejecutor);
}
