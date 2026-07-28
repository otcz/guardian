package guardian.service.admin;

import guardian.dto.admin.UsuarioRequest;
import guardian.dto.admin.UsuarioResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

public interface UsuarioService {

    List<UsuarioResponse> listar(Long conjuntoId);

    /**
     * Da de alta la cuenta de una persona con clave inicial igual a su documento.
     *
     * <p>Nace <b>inactiva</b>: si naciera activa, cualquiera que conozca la
     * cedula de un vecino podria tomar la cuenta antes que su dueno. El
     * administrador la habilita cuando entrega el acceso en mano.</p>
     */
    UsuarioResponse crear(UsuarioRequest request, UsuarioAutenticado ejecutor);

    UsuarioResponse cambiarRol(Long id, String rol, UsuarioAutenticado ejecutor);

    UsuarioResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor);

    /** Devuelve la clave al documento y vuelve a exigir el cambio en el proximo ingreso. */
    UsuarioResponse restablecerClave(Long id, UsuarioAutenticado ejecutor);
}
