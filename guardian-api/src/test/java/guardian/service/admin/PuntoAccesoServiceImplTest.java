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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las porterias.
 *
 * <p>Lo unico verdaderamente delicado aca es apagarlas. Cada evento de la
 * bitacora apunta a la porteria por la que paso la persona; si no queda
 * ninguna activa, la fabrica de eventos deja ese dato en nulo SIN AVISAR y el
 * conjunto sigue operando. El dia que alguien pregunte por donde entro un
 * visitante, la respuesta ya no existe.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PuntoAccesoServiceImplTest {

    private static final Long CONJUNTO = 7L;

    @Mock
    private GdPuntoAccesoRepository puntoAccesoRepository;

    @Mock
    private GdConjuntoRepository conjuntoRepository;

    @Mock
    private GdAccesoEventoRepository accesoEventoRepository;

    @Mock
    private guardian.repository.GdGuardiaPorteriaRepository guardiaPorteriaRepository;

    @InjectMocks
    private PuntoAccesoServiceImpl servicio;

    // ── Apagar la ultima ─────────────────────────────────────────────────────

    @Test
    @DisplayName("no deja apagar la unica porteria activa")
    void noApagaLaUltima() {
        GdPuntoAcceso unica = porteria(1L, "Porteria principal", Codigos.SI);
        when(puntoAccesoRepository.findById(1L)).thenReturn(Optional.of(unica));
        when(puntoAccesoRepository.countByConjuntoIdAndActivo(CONJUNTO, Codigos.SI)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.cambiarEstado(1L, false, ejecutor()))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.PORTERIA_ULTIMA_ACTIVA);

        verify(puntoAccesoRepository, never()).save(any(GdPuntoAcceso.class));
    }

    @Test
    @DisplayName("con dos activas si deja apagar una")
    void apagaCuandoQuedaOtra() {
        GdPuntoAcceso una = porteria(1L, "Porteria norte", Codigos.SI);
        when(puntoAccesoRepository.findById(1L)).thenReturn(Optional.of(una));
        when(puntoAccesoRepository.countByConjuntoIdAndActivo(CONJUNTO, Codigos.SI)).thenReturn(2L);
        when(puntoAccesoRepository.save(any(GdPuntoAcceso.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        PuntoAccesoResponse respuesta = servicio.cambiarEstado(1L, false, ejecutor());

        assertThat(respuesta.getActivo()).isEqualTo(Codigos.NO);
    }

    @Test
    @DisplayName("volver a encender no cuenta como apagar")
    void encenderNoSeBloquea() {
        // El conteo de activas es 0 —esta apagada y es la unica—, pero la
        // guarda solo aplica al apagar. Sin esta distincion, un conjunto que
        // quedara sin porterias activas no podria encender ninguna.
        GdPuntoAcceso apagada = porteria(1L, "Porteria de servicio", Codigos.NO);
        when(puntoAccesoRepository.findById(1L)).thenReturn(Optional.of(apagada));
        when(puntoAccesoRepository.countByConjuntoIdAndActivo(CONJUNTO, Codigos.SI)).thenReturn(0L);
        when(puntoAccesoRepository.save(any(GdPuntoAcceso.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        assertThatCode(() -> servicio.cambiarEstado(1L, true, ejecutor()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("apagar una que ya estaba apagada no dispara la guarda")
    void apagarUnaYaApagadaNoDisparaLaGuarda() {
        GdPuntoAcceso apagada = porteria(1L, "Porteria de servicio", Codigos.NO);
        when(puntoAccesoRepository.findById(1L)).thenReturn(Optional.of(apagada));
        when(puntoAccesoRepository.countByConjuntoIdAndActivo(CONJUNTO, Codigos.SI)).thenReturn(1L);
        when(puntoAccesoRepository.save(any(GdPuntoAcceso.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        // La activa es OTRA. Contar 1 y rechazar aca dejaria sin poder tocar
        // una porteria que no aporta nada al conteo.
        assertThatCode(() -> servicio.cambiarEstado(1L, false, ejecutor()))
                .doesNotThrowAnyException();
    }

    // ── Aislamiento entre sedes ──────────────────────────────────────────────

    @Test
    @DisplayName("la porteria de otro conjunto no existe para este administrador")
    void noTocaPorteriasDeOtraSede() {
        GdPuntoAcceso ajena = porteria(9L, "Porteria de otra sede", Codigos.SI);
        ajena.getConjunto().setId(99L);
        when(puntoAccesoRepository.findById(9L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> servicio.cambiarEstado(9L, false, ejecutor()))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.NO_ENCONTRADO);
    }

    // ── Nombres ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dos porterias no pueden llamarse igual en el mismo conjunto")
    void nombreUnicoPorConjunto() {
        when(puntoAccesoRepository.findFirstByConjuntoIdAndNombreIgnoreCase(CONJUNTO, "Porteria norte"))
                .thenReturn(Optional.of(porteria(3L, "Porteria norte", Codigos.SI)));

        assertThatThrownBy(() -> servicio.crear(peticion("  Porteria norte  "), ejecutor()))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.PORTERIA_YA_REGISTRADA);
    }

    @Test
    @DisplayName("renombrarse a si misma no choca consigo misma")
    void renombrarLaMismaNoChoca() {
        GdPuntoAcceso propia = porteria(3L, "Porteria norte", Codigos.SI);
        when(puntoAccesoRepository.findById(3L)).thenReturn(Optional.of(propia));
        when(puntoAccesoRepository.findFirstByConjuntoIdAndNombreIgnoreCase(CONJUNTO, "Porteria norte"))
                .thenReturn(Optional.of(propia));
        when(puntoAccesoRepository.save(any(GdPuntoAcceso.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        assertThatCode(() -> servicio.actualizar(3L, peticion("Porteria norte"), ejecutor()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sin marcar vehiculos, la porteria los permite")
    void permiteVehiculoPorDefecto() {
        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(CONJUNTO);
        when(conjuntoRepository.findById(CONJUNTO)).thenReturn(Optional.of(conjunto));
        when(puntoAccesoRepository.findFirstByConjuntoIdAndNombreIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(puntoAccesoRepository.save(any(GdPuntoAcceso.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        PuntoAccesoRequest sinMarcar = peticion("Porteria nueva");
        sinMarcar.setPermiteVehiculo(null);

        assertThat(servicio.crear(sinMarcar, ejecutor()).getPermiteVehiculo())
                .isEqualTo(Codigos.SI);
    }

    @Test
    @DisplayName("una direccion en blanco se guarda como sin dato")
    void direccionEnBlancoEsNula() {
        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(CONJUNTO);
        when(conjuntoRepository.findById(CONJUNTO)).thenReturn(Optional.of(conjunto));
        when(puntoAccesoRepository.findFirstByConjuntoIdAndNombreIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(puntoAccesoRepository.save(any(GdPuntoAcceso.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        PuntoAccesoRequest enBlanco = peticion("Porteria nueva");
        enBlanco.setDireccion("   ");

        assertThat(servicio.crear(enBlanco, ejecutor()).getDireccion()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdPuntoAcceso porteria(Long id, String nombre, String activo) {
        GdConjunto conjunto = new GdConjunto();
        conjunto.setId(CONJUNTO);

        GdPuntoAcceso porteria = new GdPuntoAcceso();
        porteria.setId(id);
        porteria.setConjunto(conjunto);
        porteria.setNombre(nombre);
        porteria.setPermiteVehiculo(Codigos.SI);
        porteria.setActivo(activo);
        porteria.setBloqueado(Codigos.NO);
        return porteria;
    }

    private PuntoAccesoRequest peticion(String nombre) {
        PuntoAccesoRequest request = new PuntoAccesoRequest();
        request.setNombre(nombre);
        request.setDireccion("Carrera 15 # 23-40");
        request.setPermiteVehiculo(Codigos.SI);
        return request;
    }

    private UsuarioAutenticado ejecutor() {
        return new UsuarioAutenticado(1L, 10L, CONJUNTO, "1074502938", "Alguien",
                Codigos.ROL_ADMIN);
    }
}
