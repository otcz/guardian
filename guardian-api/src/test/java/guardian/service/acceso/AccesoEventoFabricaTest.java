package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.entity.acceso.GdAccesoEvento;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.conjunto.GdPuntoAcceso;
import guardian.repository.GdAccesoEventoRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdPuntoAccesoRepository;
import guardian.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Por donde paso la persona.
 *
 * <p>El puntoAccesoId llega de la TABLET, no del token: es entrada no
 * confiable. Un findById suelto deja estampar la porteria de otra sede sobre un
 * evento de esta, y eso no deja la bitacora incompleta sino MENTIROSA. Estas
 * pruebas cubren esa frontera y el silencio que la tapaba.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccesoEventoFabricaTest {

    private static final Long SEDE = 4L;

    @Mock
    private GdAccesoEventoRepository eventoRepository;

    @Mock
    private GdPersonaRepository personaRepository;

    @Mock
    private GdPuntoAccesoRepository puntoAccesoRepository;

    @InjectMocks
    private AccesoEventoFabrica fabrica;

    @Test
    @DisplayName("la porteria que pide la tablet se resuelve DENTRO de la sede")
    void resuelveDentroDeLaSede() {
        GdPuntoAcceso propia = porteria(2L, "Porteria norte");
        when(puntoAccesoRepository.findByIdAndConjuntoId(2L, SEDE))
                .thenReturn(Optional.of(propia));

        GdAccesoEvento evento = fabrica.nuevoEvento(guardia(), 2L);

        assertThat(evento.getPuntoAcceso()).isSameAs(propia);
        // El findById suelto no se usa NUNCA: es justamente el que no filtra.
        verify(puntoAccesoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("un id de OTRA sede no se estampa: cae al punto por defecto")
    void noEstampaLaPorteriaDeOtraSede() {
        // La tablet quedo con la porteria de otra sede guardada. El repositorio
        // filtrado no la encuentra, que es exactamente lo que tiene que pasar.
        when(puntoAccesoRepository.findByIdAndConjuntoId(99L, SEDE))
                .thenReturn(Optional.empty());
        GdPuntoAcceso unica = porteria(2L, "Porteria principal");
        when(puntoAccesoRepository.findByConjuntoIdAndActivoOrderByNombreAsc(SEDE, Codigos.SI))
                .thenReturn(Collections.singletonList(unica));

        GdAccesoEvento evento = fabrica.nuevoEvento(guardia(), 99L);

        assertThat(evento.getPuntoAcceso()).isSameAs(unica);
    }

    @Test
    @DisplayName("sin id y con una sola activa, se usa esa")
    void conUnaSolaNoHaceFaltaPreguntar() {
        GdPuntoAcceso unica = porteria(2L, "Porteria principal");
        when(puntoAccesoRepository.findByConjuntoIdAndActivoOrderByNombreAsc(SEDE, Codigos.SI))
                .thenReturn(Collections.singletonList(unica));

        assertThat(fabrica.nuevoEvento(guardia(), null).getPuntoAcceso()).isSameAs(unica);
    }

    @Test
    @DisplayName("sin id y con DOS activas queda nulo: adivinar seria peor")
    void conDosActivasNoAdivina() {
        when(puntoAccesoRepository.findByConjuntoIdAndActivoOrderByNombreAsc(SEDE, Codigos.SI))
                .thenReturn(Arrays.asList(porteria(2L, "Norte"), porteria(3L, "Sur")));

        // Un nulo se lee despues como "no se supo". Un nombre inventado se
        // leeria como un hecho, y seria falso la mitad de las veces.
        assertThat(fabrica.nuevoEvento(guardia(), null).getPuntoAcceso()).isNull();
    }

    @Test
    @DisplayName("un id valido de la sede gana sobre el default aunque haya varias")
    void elIdDeLaTabletManda() {
        GdPuntoAcceso sur = porteria(3L, "Sur");
        when(puntoAccesoRepository.findByIdAndConjuntoId(3L, SEDE)).thenReturn(Optional.of(sur));

        assertThat(fabrica.nuevoEvento(guardia(), 3L).getPuntoAcceso()).isSameAs(sur);
        verify(puntoAccesoRepository, never())
                .findByConjuntoIdAndActivoOrderByNombreAsc(eq(SEDE), any());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdPuntoAcceso porteria(Long id, String nombre) {
        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(SEDE);

        GdPuntoAcceso porteria = new GdPuntoAcceso();
        porteria.setId(id);
        porteria.setConjunto(conjunto);
        porteria.setNombre(nombre);
        porteria.setActivo(Codigos.SI);
        return porteria;
    }

    private UsuarioAutenticado guardia() {
        return new UsuarioAutenticado(1L, 10L, SEDE, "1074502938", "Un Guardia",
                Codigos.ROL_GUARDIA);
    }
}
