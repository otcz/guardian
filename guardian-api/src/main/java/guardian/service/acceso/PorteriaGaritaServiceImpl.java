package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.dto.acceso.PorteriaGaritaResponse;
import guardian.dto.acceso.PorteriasGaritaResponse;
import guardian.entity.conjunto.GdPuntoAcceso;
import guardian.repository.GdGuardiaPorteriaRepository;
import guardian.repository.GdPuntoAccesoRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PorteriaGaritaServiceImpl implements PorteriaGaritaService {

    private final GdPuntoAccesoRepository puntoAccesoRepository;
    private final GdGuardiaPorteriaRepository guardiaPorteriaRepository;

    @Override
    @Transactional(readOnly = true)
    public PorteriasGaritaResponse disponibles(UsuarioAutenticado guardia) {
        List<GdPuntoAcceso> activas = puntoAccesoRepository
                .findByConjuntoIdAndActivoOrderByNombreAsc(guardia.getConjuntoId(), Codigos.SI);

        Set<Long> asignadas = new HashSet<>(guardiaPorteriaRepository.porteriasAsignadasA(
                guardia.getPersonaId(), guardia.getConjuntoId(), Codigos.SI));

        List<PorteriaGaritaResponse> opciones = activas.stream()
                .map(p -> PorteriaGaritaResponse.builder()
                        .id(p.getId())
                        .nombre(p.getNombre())
                        .direccion(p.getDireccion())
                        .permiteVehiculo(p.getPermiteVehiculo())
                        .asignada(asignadas.contains(p.getId()))
                        .build())
                .collect(Collectors.toList());

        return PorteriasGaritaResponse.builder()
                .porterias(opciones)
                .sugeridaId(sugerir(opciones))
                .build();
    }

    /**
     * Cual proponer.
     *
     * <p>Con una sola porteria activa, esa: preguntarle al guardia entre una
     * unica opcion es un toque que no decide nada. Con varias, la suya solo si
     * tiene EXACTAMENTE una asignada — si le tocan dos puertas, adivinar seria
     * marcar sus registros con la puerta equivocada la mitad del tiempo, y es
     * mejor que elija.</p>
     */
    private Long sugerir(List<PorteriaGaritaResponse> opciones) {
        if (opciones.size() == 1) {
            return opciones.get(0).getId();
        }
        List<PorteriaGaritaResponse> suyas = opciones.stream()
                .filter(PorteriaGaritaResponse::isAsignada)
                .collect(Collectors.toList());
        return suyas.size() == 1 ? suyas.get(0).getId() : null;
    }
}
