package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.entity.acceso.GdAccesoEvento;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdPersonaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** La presencia se deriva del ULTIMO evento permitido: entrada = adentro. */
@ExtendWith(MockitoExtension.class)
class PresenciaServiceImplTest {

    @Mock private GdAccesoEventoRepository eventoRepository;
    @Mock private GdPersonaRepository personaRepository;

    @InjectMocks
    private PresenciaServiceImpl servicio;

    private GdAccesoEvento eventoCon(String sentido) {
        GdAccesoEvento evento = new GdAccesoEvento();
        evento.setSentido(sentido);
        evento.setResultado(Codigos.RESULTADO_PERMITIDO);
        return evento;
    }

    @Test
    @DisplayName("ultimo evento ENTRADA -> esta adentro")
    void ultimaEntradaSignificaAdentro() {
        when(eventoRepository.findFirstByPersonaIdAndResultadoOrderByFechaEventoDesc(
                50L, Codigos.RESULTADO_PERMITIDO))
                .thenReturn(Optional.of(eventoCon(Codigos.ENTRADA)));

        assertThat(servicio.estaAdentro(50L)).isTrue();
    }

    @Test
    @DisplayName("ultimo evento SALIDA -> esta afuera")
    void ultimaSalidaSignificaAfuera() {
        when(eventoRepository.findFirstByPersonaIdAndResultadoOrderByFechaEventoDesc(
                50L, Codigos.RESULTADO_PERMITIDO))
                .thenReturn(Optional.of(eventoCon(Codigos.SALIDA)));

        assertThat(servicio.estaAdentro(50L)).isFalse();
    }

    @Test
    @DisplayName("sin eventos -> afuera (el estado inicial de todo el mundo)")
    void sinEventosEstaAfuera() {
        when(eventoRepository.findFirstByPersonaIdAndResultadoOrderByFechaEventoDesc(
                50L, Codigos.RESULTADO_PERMITIDO))
                .thenReturn(Optional.empty());

        assertThat(servicio.estaAdentro(50L)).isFalse();
    }

    @Test
    @DisplayName("mismas reglas para el invitado")
    void presenciaDeInvitado() {
        when(eventoRepository.findFirstByInvitacionIdAndResultadoOrderByFechaEventoDesc(
                20L, Codigos.RESULTADO_PERMITIDO))
                .thenReturn(Optional.of(eventoCon(Codigos.ENTRADA)));

        assertThat(servicio.estaAdentroInvitado(20L)).isTrue();
    }
}
