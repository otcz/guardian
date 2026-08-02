package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.dto.acceso.PresenciaResponse;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdPersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class PresenciaServiceImpl implements PresenciaService {

    private static final long MILIS_POR_HORA = 3_600_000L;

    private final GdAccesoEventoRepository eventoRepository;
    private final GdPersonaRepository personaRepository;

    /**
     * Ventana del conteo de invitados adentro. Un invitado que salio caminando
     * sin escanear no debe inflar el conteo de evacuacion para siempre.
     */
    @Value("${guardian.acceso.horas-presencia-invitado}")
    private long horasPresenciaInvitado;

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
        return conteo(conjuntoId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PresenciaResponse conteo(Long conjuntoId, Long exceptoPersonaId) {
        long personasAdentro = eventoRepository.contarAdentro(
                conjuntoId, Codigos.RESULTADO_PERMITIDO, Codigos.ENTRADA);
        Date piso = new Date(System.currentTimeMillis()
                - horasPresenciaInvitado * MILIS_POR_HORA);
        long invitadosAdentro = eventoRepository.contarInvitadosAdentro(
                conjuntoId, Codigos.RESULTADO_PERMITIDO, Codigos.ENTRADA, piso);

        // La exclusion aplica a la POBLACION, no a quien esta adentro: alguien
        // que nunca ha escaneado no puede estar adentro, y si escaneara,
        // fisicamente esta ahi y para una evacuacion cuenta.
        long totalActivos = exceptoPersonaId == null
                ? personaRepository.countByConjuntoIdAndActivo(conjuntoId, Codigos.SI)
                : personaRepository.countByConjuntoIdAndActivoAndIdNot(
                        conjuntoId, Codigos.SI, exceptoPersonaId);

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
