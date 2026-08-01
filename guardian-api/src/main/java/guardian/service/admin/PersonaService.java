package guardian.service.admin;

import guardian.dto.admin.PersonaRequest;
import guardian.dto.admin.PersonaResponse;
import guardian.security.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonaService {

    Page<PersonaResponse> buscar(Long conjuntoId, String texto, Pageable pageable);

    PersonaResponse obtener(Long id, Long conjuntoId);

    PersonaRegistrada crear(PersonaRequest request, UsuarioAutenticado ejecutor);

    PersonaResponse actualizar(Long id, PersonaRequest request, UsuarioAutenticado ejecutor);

    PersonaResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor);

    /** Emite (o reemite) la credencial QR. Exige que la persona tenga foto. */
    String emitirCredencial(Long id, UsuarioAutenticado ejecutor);

    byte[] credencialPng(Long id, Long conjuntoId, int tamanoPx);

    /**
     * Revoca la credencial activa sin emitir otra. Es el freno de emergencia
     * cuando un QR se compromete: surte efecto en el siguiente escaneo.
     */
    void revocarCredencial(Long id, UsuarioAutenticado ejecutor);

    /**
     * Eliminacion FISICA — exclusiva del administrador; los residentes solo
     * inactivan. La bitacora sobrevive: los eventos conservan nombre y
     * documento copiados y solo se anula la FK.
     */
    void eliminar(Long id, UsuarioAutenticado ejecutor);
}
