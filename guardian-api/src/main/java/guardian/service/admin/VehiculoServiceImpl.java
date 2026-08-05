package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.VehiculoRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.security.Autoridad;
import guardian.security.UsuarioAutenticado;
import guardian.util.CatalogoVehiculo;
import guardian.util.PlacaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehiculoServiceImpl implements VehiculoService {

    private final GdVehiculoRepository vehiculoRepository;
    private final GdCasaRepository casaRepository;
    private final GdAccesoEventoRepository eventoRepository;
    private final ParametroService parametroService;
    private final EtiquetaCatalogoService etiquetaCatalogoService;

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar(Long conjuntoId) {
        return vehiculoRepository.findByConjuntoIdOrderByPlacaAsc(conjuntoId)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarPorCasa(Long casaId, Long conjuntoId) {
        obtenerCasa(casaId, conjuntoId);
        return vehiculoRepository.findByCasaIdAndActivoOrderByPlacaAsc(casaId, Codigos.SI)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarPorCasaIncluyendoInactivos(Long casaId, Long conjuntoId) {
        obtenerCasa(casaId, conjuntoId);
        return vehiculoRepository.findByCasaIdOrderByPlacaAsc(casaId)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehiculoResponse crear(VehiculoRequest request, UsuarioAutenticado ejecutor) {
        String placa = PlacaUtil.normalizar(request.getPlaca());
        validarCatalogo(request.getTipo(), request.getMarca(), request.getColor());

        // Placa unica en TODO el sistema: el mismo carro no puede estar en dos
        // sedes ni en dos casas, o la bitacora deja de ser confiable.
        if (vehiculoRepository.findByPlaca(placa).isPresent()) {
            throw GuardianException.conflicto(MensajesGlobales.PLACA_YA_REGISTRADA);
        }

        GdCasa casa = obtenerCasa(request.getCasaId(), ejecutor.getConjuntoId());

        GdVehiculo vehiculo = new GdVehiculo();
        vehiculo.setConjuntoId(ejecutor.getConjuntoId());
        vehiculo.setCasa(casa);
        vehiculo.setPlaca(placa);
        aplicar(vehiculo, request);
        vehiculo.setActivo(Codigos.SI);
        vehiculo.setUsuarioCreador(ejecutor.getDocumento());

        GdVehiculo guardado = vehiculoRepository.save(vehiculo);
        log.info("[admin] vehiculo creado id={} placa={} casaId={}",
                guardado.getId(), placa, casa.getId());
        return mapear(guardado);
    }

    @Override
    @Transactional
    public VehiculoResponse actualizar(Long id, VehiculoRequest request, UsuarioAutenticado ejecutor) {
        GdVehiculo vehiculo = obtener(id, ejecutor.getConjuntoId());
        String placa = PlacaUtil.normalizar(request.getPlaca());
        validarCatalogo(request.getTipo(), request.getMarca(), request.getColor());

        vehiculoRepository.findByPlaca(placa)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw GuardianException.conflicto(MensajesGlobales.PLACA_YA_REGISTRADA);
                });

        vehiculo.setCasa(obtenerCasa(request.getCasaId(), ejecutor.getConjuntoId()));
        vehiculo.setPlaca(placa);
        aplicar(vehiculo, request);
        vehiculo.setUsuarioModificador(ejecutor.getDocumento());

        return mapear(vehiculoRepository.save(vehiculo));
    }

    @Override
    @Transactional
    public VehiculoResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado ejecutor) {
        GdVehiculo vehiculo = obtener(id, ejecutor.getConjuntoId());
        vehiculo.setActivo(activo ? Codigos.SI : Codigos.NO);
        vehiculo.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] vehiculo id={} activo={} por={}", id, vehiculo.getActivo(),
                ejecutor.getDocumento());
        return mapear(vehiculoRepository.save(vehiculo));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioAutenticado ejecutor) {
        Autoridad.exigirSuperAdmin(ejecutor);

        GdVehiculo vehiculo = obtener(id, ejecutor.getConjuntoId());

        eventoRepository.desvincularVehiculo(id);
        vehiculoRepository.delete(vehiculo);

        log.warn("[admin] vehiculo ELIMINADO id={} placa={} por={}",
                id, vehiculo.getPlaca(), ejecutor.getDocumento());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdVehiculo obtener(Long id, Long conjuntoId) {
        GdVehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!vehiculo.getConjuntoId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return vehiculo;
    }

    private GdCasa obtenerCasa(Long casaId, Long conjuntoId) {
        GdCasa casa = casaRepository.findById(casaId)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        if (!casa.getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return casa;
    }

    private void aplicar(GdVehiculo vehiculo, VehiculoRequest request) {
        vehiculo.setTipo(request.getTipo());
        vehiculo.setMarca(CatalogoVehiculo.limpiar(request.getMarca()));
        vehiculo.setColor(CatalogoVehiculo.limpiar(request.getColor()));
    }

    private void validarCatalogo(String tipo, String marca, String color) {
        CatalogoVehiculo.exigir(parametroService, tipo, marca, color);
    }

    private VehiculoResponse mapear(GdVehiculo vehiculo) {
        return VehiculoResponse.builder()
                .id(vehiculo.getId())
                .placa(vehiculo.getPlaca())
                .tipo(vehiculo.getTipo())
                .marca(vehiculo.getMarca())
                .color(vehiculo.getColor())
                .tipoNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_TIPO_VEHICULO, vehiculo.getTipo()))
                .marcaNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_MARCA_VEHICULO, vehiculo.getMarca()))
                .colorNombre(etiquetaCatalogoService
                        .etiqueta(Codigos.GRUPO_COLOR_VEHICULO, vehiculo.getColor()))
                .activo(vehiculo.getActivo())
                .bloqueado(vehiculo.getBloqueado())
                .motivoBloqueo(vehiculo.getMotivoBloqueo())
                .casaId(vehiculo.getCasa().getId())
                .casaIdentificador(vehiculo.getCasa().getIdentificador())
                .build();
    }
}
