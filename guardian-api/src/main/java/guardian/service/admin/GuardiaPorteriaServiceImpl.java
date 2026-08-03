package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.AsignarGuardiasRequest;
import guardian.dto.admin.GuardiaPorteriaResponse;
import guardian.dto.admin.PuntoAccesoResponse;
import guardian.entity.conjunto.GdGuardiaPorteria;
import guardian.entity.conjunto.GdPuntoAcceso;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.exception.GuardianException;
import guardian.repository.GdGuardiaPorteriaRepository;
import guardian.repository.GdPuntoAccesoRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuardiaPorteriaServiceImpl implements GuardiaPorteriaService {

    private final GdGuardiaPorteriaRepository guardiaPorteriaRepository;
    private final GdPuntoAccesoRepository puntoAccesoRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final PuntoAccesoService puntoAccesoService;

    @Override
    @Transactional(readOnly = true)
    public List<GuardiaPorteriaResponse> listar(Long porteriaId, UsuarioAutenticado ejecutor) {
        GdPuntoAcceso porteria = porteriaDeLaSede(porteriaId, ejecutor.getConjuntoId());

        // Orden de insercion: primero los guardias actuales de la sede, ya
        // ordenados por apellido, y despues los asignados que ya no lo son.
        Map<Long, GuardiaPorteriaResponse.GuardiaPorteriaResponseBuilder> filas =
                new LinkedHashMap<>();

        for (GdUsuario cuenta : guardiasDeLaSede(ejecutor.getConjuntoId())) {
            GdPersona persona = cuenta.getPersona();
            filas.put(persona.getId(), fila(persona).esGuardia(true));
        }

        for (GdGuardiaPorteria vinculo
                : guardiaPorteriaRepository.listarDeLaPorteria(porteria.getId())) {
            if (!vinculo.estaActivo()) {
                continue;
            }
            GdPersona persona = vinculo.getPersona();
            // Asignado y sin cuenta de guardia: le cambiaron el rol despues.
            // Se muestra igual, o la fila quedaria invisible e imposible de
            // quitar desde la pantalla.
            filas.computeIfAbsent(persona.getId(), id -> fila(persona).esGuardia(false));
            filas.get(persona.getId()).asignado(true);
        }

        return filas.values().stream()
                .map(GuardiaPorteriaResponse.GuardiaPorteriaResponseBuilder::build)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PuntoAccesoResponse asignar(Long porteriaId, AsignarGuardiasRequest request,
                                       UsuarioAutenticado ejecutor) {
        GdPuntoAcceso porteria = porteriaDeLaSede(porteriaId, ejecutor.getConjuntoId());
        Set<Long> pedidos = new java.util.LinkedHashSet<>(request.getPersonaIds());

        exigirQueSeanGuardiasDeLaSede(pedidos, ejecutor.getConjuntoId());

        List<GdGuardiaPorteria> existentes =
                guardiaPorteriaRepository.listarDeLaPorteria(porteria.getId());
        Set<Long> yaConFila = existentes.stream()
                .map(v -> v.getPersona().getId())
                .collect(Collectors.toSet());

        // Reactivar o apagar las filas que ya existian. Nunca borrar: la
        // asignacion de ayer es lo que se consulta el dia de un incidente.
        for (GdGuardiaPorteria vinculo : existentes) {
            String debeQuedar = pedidos.contains(vinculo.getPersona().getId())
                    ? Codigos.SI : Codigos.NO;
            if (!debeQuedar.equals(vinculo.getActivo())) {
                vinculo.setActivo(debeQuedar);
                vinculo.setUsuarioModificador(ejecutor.getDocumento());
                guardiaPorteriaRepository.save(vinculo);
            }
        }

        for (Long personaId : pedidos) {
            if (yaConFila.contains(personaId)) {
                continue;
            }
            GdGuardiaPorteria vinculo = new GdGuardiaPorteria();
            vinculo.setPuntoAcceso(porteria);
            vinculo.setPersona(usuarioRepository.findByPersonaId(personaId)
                    .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO))
                    .getPersona());
            vinculo.setActivo(Codigos.SI);
            vinculo.setBloqueado(Codigos.NO);
            vinculo.setUsuarioCreador(ejecutor.getDocumento());
            guardiaPorteriaRepository.save(vinculo);
        }

        log.info("[admin] porteria id={} queda con {} guardias por={}",
                porteriaId, pedidos.size(), ejecutor.getDocumento());

        return puntoAccesoService.listar(ejecutor.getConjuntoId()).stream()
                .filter(p -> p.getId().equals(porteriaId))
                .findFirst()
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * El id llega por la URL: sin este filtro un administrador podria repartir
     * guardias en las porterias de otra sede.
     */
    private GdPuntoAcceso porteriaDeLaSede(Long porteriaId, Long conjuntoId) {
        return puntoAccesoRepository.findByIdAndConjuntoId(porteriaId, conjuntoId)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }

    private List<GdUsuario> guardiasDeLaSede(Long conjuntoId) {
        return usuarioRepository.listarPorConjuntoYRol(conjuntoId, Codigos.ROL_GUARDIA);
    }

    /**
     * Ninguna persona de otra sede, y ninguna que no sea guardia.
     *
     * <p>Sin esto un PUT con un id ajeno deja una fila que cruza conjuntos, y
     * nada aguas abajo la detecta: la porteria SI es de la sede del token, que
     * es lo unico que valida el resto del flujo.</p>
     */
    private void exigirQueSeanGuardiasDeLaSede(Set<Long> personaIds, Long conjuntoId) {
        if (personaIds.isEmpty()) {
            return;
        }
        Set<Long> permitidas = guardiasDeLaSede(conjuntoId).stream()
                .map(u -> u.getPersona().getId())
                .collect(Collectors.toSet());

        List<Long> intrusas = new ArrayList<>(personaIds);
        intrusas.removeAll(permitidas);
        if (!intrusas.isEmpty()) {
            log.warn("[admin] intento de asignar a la porteria personas que no son"
                    + " guardias de la sede {}: {}", conjuntoId, intrusas);
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
    }

    private GuardiaPorteriaResponse.GuardiaPorteriaResponseBuilder fila(GdPersona persona) {
        return GuardiaPorteriaResponse.builder()
                .personaId(persona.getId())
                .nombreCompleto(persona.getNombres() + " " + persona.getApellidos())
                .documento(persona.getDocumento())
                .asignado(false);
    }
}
