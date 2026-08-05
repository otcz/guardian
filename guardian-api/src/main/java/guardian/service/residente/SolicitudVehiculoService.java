package guardian.service.residente;

import guardian.dto.residente.SolicitudVehiculoResponse;
import guardian.dto.residente.VehiculoResidenteRequest;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/**
 * El lado del residente en la autorizacion de vehiculos: pedir, ver en que va
 * y quitar de la pantalla lo que ya le respondieron.
 */
public interface SolicitudVehiculoService {

    /** Lo pendiente y lo rechazado sin descartar, de la casa del solicitante. */
    List<SolicitudVehiculoResponse> misSolicitudes(UsuarioAutenticado solicitante);

    /** Pide que le registren un vehiculo. Solo el titular de la casa. */
    SolicitudVehiculoResponse solicitar(VehiculoResidenteRequest request,
                                        UsuarioAutenticado solicitante);

    /** Quita de la pantalla una solicitud rechazada que ya leyo. */
    void descartar(Long id, UsuarioAutenticado solicitante);
}
