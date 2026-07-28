package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.CasaRequest;
import guardian.dto.admin.CasaResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.conjunto.GdConjunto;
import guardian.exception.GuardianException;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdVehiculoRepository;
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
public class CasaServiceImpl implements CasaService {

    private final GdCasaRepository casaRepository;
    private final GdConjuntoRepository conjuntoRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdVehiculoRepository vehiculoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CasaResponse> listar(Long conjuntoId) {
        return casaRepository.findByConjuntoIdOrderByIdentificadorAsc(conjuntoId)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CasaResponse crear(CasaRequest request, UsuarioAutenticado ejecutor) {
        String identificador = construirIdentificador(request);

        if (casaRepository.existsByConjuntoIdAndIdentificador(ejecutor.getConjuntoId(), identificador)) {
            throw GuardianException.conflicto(MensajesGlobales.CASA_YA_REGISTRADA);
        }

        GdConjunto conjunto = conjuntoRepository.findById(ejecutor.getConjuntoId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        GdCasa casa = new GdCasa();
        casa.setConjunto(conjunto);
        aplicar(casa, request, identificador);
        casa.setActivo(Codigos.SI);
        casa.setUsuarioCreador(ejecutor.getDocumento());

        GdCasa guardada = casaRepository.save(casa);
        log.info("[admin] casa creada id={} identificador={}", guardada.getId(), identificador);
        return mapear(guardada);
    }

    @Override
    @Transactional
    public CasaResponse actualizar(Long id, CasaRequest request, UsuarioAutenticado ejecutor) {
        GdCasa casa = obtener(id, ejecutor.getConjuntoId());
        String identificador = construirIdentificador(request);

        casaRepository.findByConjuntoIdAndIdentificador(ejecutor.getConjuntoId(), identificador)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw GuardianException.conflicto(MensajesGlobales.CASA_YA_REGISTRADA);
                });

        aplicar(casa, request, identificador);
        casa.setUsuarioModificador(ejecutor.getDocumento());

        return mapear(casaRepository.save(casa));
    }

    @Override
    @Transactional
    public CasaResponse cambiarEstado(Long id, boolean activa, UsuarioAutenticado ejecutor) {
        GdCasa casa = obtener(id, ejecutor.getConjuntoId());
        casa.setActivo(activa ? Codigos.SI : Codigos.NO);
        casa.setUsuarioModificador(ejecutor.getDocumento());

        log.info("[admin] casa id={} activo={} por={}", id, casa.getActivo(), ejecutor.getDocumento());
        return mapear(casaRepository.save(casa));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdCasa obtener(Long id, Long conjuntoId) {
        GdCasa casa = casaRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        // El conjunto del token manda. Sin esta comprobacion, un administrador
        // podria editar casas de otro conjunto pasando un id ajeno en la URL.
        if (!casa.getConjunto().getId().equals(conjuntoId)) {
            throw GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO);
        }
        return casa;
    }

    /**
     * "TORRE-NUMERO" o solo el numero si el conjunto no tiene torres. Se guarda
     * ya formateado porque es lo que el guardia lee en pantalla, y armarlo en
     * cada consulta lo haria depender de que capa lo pinte.
     */
    private String construirIdentificador(CasaRequest request) {
        String torre = request.getTorre() == null ? "" : request.getTorre().trim();
        String numero = request.getNumero().trim();
        return torre.isEmpty() ? numero : torre + "-" + numero;
    }

    private void aplicar(GdCasa casa, CasaRequest request, String identificador) {
        casa.setTorre(request.getTorre() == null ? null : request.getTorre().trim());
        casa.setNumero(request.getNumero().trim());
        casa.setIdentificador(identificador);
        casa.setCuposParqueadero(request.getCuposParqueadero());
        casa.setObservaciones(request.getObservaciones());
    }

    private CasaResponse mapear(GdCasa casa) {
        return CasaResponse.builder()
                .id(casa.getId())
                .identificador(casa.getIdentificador())
                .torre(casa.getTorre())
                .numero(casa.getNumero())
                .cuposParqueadero(casa.getCuposParqueadero())
                .activo(casa.getActivo())
                .residentes(residenteCasaRepository
                        .findByCasaIdAndActivo(casa.getId(), Codigos.SI).size())
                .vehiculos(vehiculoRepository
                        .findByCasaIdAndActivoOrderByPlacaAsc(casa.getId(), Codigos.SI).size())
                .build();
    }
}
