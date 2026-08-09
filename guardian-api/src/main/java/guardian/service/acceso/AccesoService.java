package guardian.service.acceso;

import guardian.dto.acceso.FiltroEventos;
import guardian.dto.acceso.AccesoEventoResponse;
import guardian.dto.acceso.FichaVerificacionResponse;
import guardian.dto.acceso.RegistrarAccesoRequest;
import guardian.dto.acceso.VerificarDocumentoRequest;
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

    /**
     * La misma ficha, pero llegando por el documento en vez de por el QR.
     *
     * <p>Es el camino que comparten el lector de codigo de barras —del PDF417
     * de una cedula sale el numero, no un payload de GUARDIAN— y el guardia
     * tecleando la cedula.</p>
     *
     * <p>Resuelve la persona —o la invitacion, si el documento es de un
     * invitado que llega sin su codigo— y despues corre EXACTAMENTE las mismas
     * reglas que el QR: bloqueos, casa, presencia y registro del intento. Lo
     * unico que cambia es como se identifico.</p>
     */
    FichaVerificacionResponse verificarPorDocumento(VerificarDocumentoRequest request,
                                                    UsuarioAutenticado guardia);

    /** Confirma el ingreso una vez el guardia eligio a pie o vehiculo. */
    AccesoEventoResponse registrar(RegistrarAccesoRequest request, UsuarioAutenticado guardia);

    /** La bitacora, acotada por lo que la pantalla haya filtrado. */
    Page<AccesoEventoResponse> buscarEventos(Long conjuntoId, FiltroEventos filtros,
                                             Pageable pageable);
}
