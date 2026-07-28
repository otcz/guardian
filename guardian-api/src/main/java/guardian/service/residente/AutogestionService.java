package guardian.service.residente;

import guardian.dto.admin.VehiculoResponse;
import guardian.dto.residente.FamiliarRequest;
import guardian.dto.residente.FamiliarResponse;
import guardian.dto.residente.VehiculoResidenteRequest;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.PersonaRegistrada;

import java.util.List;

/**
 * Autogestion del nucleo familiar y los vehiculos, hecha por el residente
 * sobre SU casa.
 *
 * <p><b>Aca no existe eliminar.</b> El residente solo activa e inactiva; la
 * eliminacion fisica es exclusiva del administrador. Un residente que borra a
 * la ex-pareja tambien borraria su historial de accesos, y ese historial no le
 * pertenece a el sino al conjunto.</p>
 */
public interface AutogestionService {

    List<FamiliarResponse> listarFamilia(UsuarioAutenticado usuario);

    /** Alta de esposa/esposo/hijo/invitado en MI casa. TITULAR esta vetado. */
    PersonaRegistrada agregarFamiliar(FamiliarRequest request, UsuarioAutenticado usuario);

    FamiliarResponse cambiarEstadoFamiliar(Long personaId, boolean activo,
                                           UsuarioAutenticado usuario);

    List<VehiculoResponse> listarMisVehiculos(UsuarioAutenticado usuario);

    VehiculoResponse agregarVehiculo(VehiculoResidenteRequest request,
                                     UsuarioAutenticado usuario);

    VehiculoResponse cambiarEstadoVehiculo(Long vehiculoId, boolean activo,
                                           UsuarioAutenticado usuario);
}
