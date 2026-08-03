package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.PuntoAccesoRequest;
import guardian.dto.admin.PuntoAccesoResponse;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.conjunto.GdPuntoAcceso;
import guardian.exception.GuardianException;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdPuntoAccesoRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PuntoAccesoServiceImpl implements PuntoAccesoService {

    private final GdPuntoAccesoRepository puntoAccesoRepository;
    private final GdConjuntoRepository conjuntoRepository;
    private final GdAccesoEventoRepository accesoEventoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PuntoAccesoResponse> listar(Long conjuntoId) {
        return puntoAccesoRepository.findByConjuntoIdOrderByNombreAsc(conjuntoId)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PuntoAccesoResponse crear(PuntoAccesoRequest request, UsuarioAutenticado ejecutor) {
        String nombre = request.getNombre().trim();
        exigirNombreLibre(ejecutor.getConjuntoId(), nombre, null);

        GdConjunto conjunto = conjuntoRepository.findById(ejecutor.getConjuntoId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        GdPuntoAcceso porteria = new GdPuntoAcceso();
        porteria.setConjunto(conjunto);
        aplicar(porteria, request, nombre);
        porteria.setActivo(Codigos.SI);
        porteria.setBloqueado(Codigos.NO);
        porteria.setUsuarioCreador(ejecutor.getDocumento());

        GdPuntoAcceso guardada = puntoAccesoRepository.save(porteria);
        log.info("[admin] porteria creada id={} nombre={} por={}",
                guardada.getId(), nombre, ejecutor.getDocumento());
        return mapear(guardada);
    }

    @Override
    @Transactional
    public PuntoAccesoResponse actualizar(Long id, PuntoAccesoRequest request,
                                          UsuarioAutenticado ejecutor) {
        GdPuntoAcceso porteria = obtener(id, ejecutor.getConjuntoId());
        String nombre = request.getNombre().trim();
        exigirNombreLibre(ejecutor.getConjuntoId(), nombre, id);

        aplicar(porteria, request, nombre);
        porteria.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] porteria actualizada id={} nombre={} por={}",
                id, nombre, ejecutor.getDocumento());
        return mapear(puntoAccesoRepository.save(porteria));
    }

    @Override
    @Transactional
    public PuntoAccesoResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor) {
        GdPuntoAcceso porteria = obtener(id, ejecutor.getConjuntoId());

        if (!activa) {
            exigirQueNoSeaLaUltima(porteria);
        }

        porteria.setActivo(activa ? Codigos.SI : Codigos.NO);
        porteria.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] porteria id={} activo={} por={}",
                id, porteria.getActivo(), ejecutor.getDocumento());
        return mapear(puntoAccesoRepository.save(porteria));
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * El conjunto del token manda: sin esta comprobacion, un administrador
     * podria editar la porteria de otra sede pasando un id ajeno en la URL.
     */
    private GdPuntoAcceso obtener(Long id, Long conjuntoId) {
        GdPuntoAcceso porteria = puntoAccesoRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!porteria.getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return porteria;
    }

    /**
     * Sin ninguna porteria activa, la fabrica de eventos no encuentra por donde
     * paso la persona y lo deja nulo sin avisar. El conjunto se queda operando
     * y el problema solo se ve meses despues, leyendo una bitacora que no
     * puede responder por donde entro nadie.
     */
    private void exigirQueNoSeaLaUltima(GdPuntoAcceso porteria) {
        if (!porteria.estaActivo()) {
            return;
        }
        long activas = puntoAccesoRepository.countByConjuntoIdAndActivo(
                porteria.getConjunto().getId(), Codigos.SI);
        if (activas <= 1) {
            throw GuardianException.conflicto(MensajesGlobales.PORTERIA_ULTIMA_ACTIVA);
        }
    }

    /**
     * Nombres unicos dentro del conjunto. Dos "Porteria norte" en el mismo
     * combo obligan al guardia a adivinar cual es la suya, y la bitacora sale
     * ambigua justo donde tenia que ser precisa.
     */
    private void exigirNombreLibre(Long conjuntoId, String nombre, Long idPropio) {
        puntoAccesoRepository.findFirstByConjuntoIdAndNombreIgnoreCase(conjuntoId, nombre)
                .filter(otra -> !otra.getId().equals(idPropio))
                .ifPresent(otra -> {
                    throw GuardianException.conflicto(MensajesGlobales.PORTERIA_YA_REGISTRADA);
                });
    }

    private void aplicar(GdPuntoAcceso porteria, PuntoAccesoRequest request, String nombre) {
        porteria.setNombre(nombre);
        porteria.setDireccion(vacioComoNulo(request.getDireccion()));
        // Nulo es "S": el caso comun es una porteria por donde entra todo.
        porteria.setPermiteVehiculo(
                Codigos.NO.equals(request.getPermiteVehiculo()) ? Codigos.NO : Codigos.SI);
        porteria.setObservaciones(vacioComoNulo(request.getObservaciones()));
    }

    /** Un campo opcional que llega en blanco es "sin dato", no una cadena vacia. */
    private String vacioComoNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private PuntoAccesoResponse mapear(GdPuntoAcceso porteria) {
        return PuntoAccesoResponse.builder()
                .id(porteria.getId())
                .nombre(porteria.getNombre())
                .direccion(porteria.getDireccion())
                .permiteVehiculo(porteria.getPermiteVehiculo())
                .activo(porteria.getActivo())
                .registros(accesoEventoRepository.countByPuntoAccesoId(porteria.getId()))
                .build();
    }
}
