package guardian.service.acceso;

import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarQrRequest;
import guardian.security.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;

public interface AccesoService {

    /**
     * Resuelve un QR escaneado y devuelve la ficha que ve el guardia.
     *
     * <p>Si el acceso se deniega, el evento se escribe <b>aca mismo</b>: el
     * guardia no va a pulsar "registrar" sobre una pantalla roja, y sin esto los
     * intentos fallidos — que son justo los que interesa auditar — nunca
     * quedarian.</p>
     */
    FichaVerificacionResponse verificar(VerificarQrRequest request, UsuarioAutenticado guardia);

    /** Confirma el ingreso una vez el guardia eligio a pie o vehiculo. */
    AccesoEventoResponse registrar(RegistrarAccesoRequest request, UsuarioAutenticado guardia);

    Page<AccesoEventoResponse> buscarEventos(Long conjuntoId, Date desde, Date hasta,
                                             Long casaId, String resultado, Pageable pageable);
}
