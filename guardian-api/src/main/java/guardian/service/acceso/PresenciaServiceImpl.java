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
    public boolean estaAdentroInvitado(Long invitacionId) {
        return eventoRepository
                .findFirstByInvitacionIdAndResultadoOrderByFechaEventoDesc(
                        invitacionId, Codigos.RESULTADO_PERMITIDO)
                .map(ultimo -> Codigos.ENTRADA.equals(ultimo.getSentido()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public PresenciaResponse conteo(Long conjuntoId) {
        long personasAdentro = eventoRepository.contarAdentro(
                conjuntoId, Codigos.RESULTADO_PERMITIDO, Codigos.ENTRADA);
        long invitadosAdentro = eventoRepository.contarInvitadosAdentro(
                conjuntoId, Codigos.RESULTADO_PERMITIDO, Codigos.ENTRADA);
        long totalActivos = personaRepository.countByConjuntoIdAndActivo(conjuntoId, Codigos.SI);

        // ADENTRO cuenta a TODO el que este dentro del conjunto, invitados
        // incluidos: ese numero responde "¿a quien evacuo en una emergencia?".
        // AFUERA solo cuenta residentes: un invitado que no ha llegado no es
        // alguien "afuera" del conjunto.
        return PresenciaResponse.builder()
                .adentro(personasAdentro + invitadosAdentro)
                .afuera(Math.max(0, totalActivos - personasAdentro))
                .totalActivos(totalActivos)
                .build();
    }
}
