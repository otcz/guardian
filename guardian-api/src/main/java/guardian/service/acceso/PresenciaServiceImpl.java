package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.dto.acceso.PresenciaResponse;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdPersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PresenciaServiceImpl implements PresenciaService {

    private final GdAccesoEventoRepository eventoRepository;
    private final GdPersonaRepository personaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean estaAdentro(Long personaId) {
        return eventoRepository
                .findFirstByPersonaIdAndResultadoOrderByFechaEventoDesc(
                        personaId, Codigos.RESULTADO_PERMITIDO)
                .map(ultimo -> Codigos.ENTRADA.equals(ultimo.getSentido()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public PresenciaResponse conteo(Long conjuntoId) {
        long adentro = eventoRepository.contarAdentro(
                conjuntoId, Codigos.RESULTADO_PERMITIDO, Codigos.ENTRADA);
        long totalActivos = personaRepository.countByConjuntoIdAndActivo(conjuntoId, Codigos.SI);

        return PresenciaResponse.builder()
                .adentro(adentro)
                .afuera(Math.max(0, totalActivos - adentro))
                .totalActivos(totalActivos)
                .build();
    }
}
