package guardian.service.acceso;

import guardian.constant.ApiEndpoint;
import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.residente.MiQrResponse;
import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.persona.GdPersona;
import guardian.exception.GuardianException;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * La credencial del residente y la foto que la desbloquea.
 *
 * <p>Sin foto no hay credencial — el guardia compara la cara con la pantalla y
 * un QR sin retrato no controla nada. Pero eso no puede dejar al residente en
 * un callejon: antes veia un aviso rojo y dependia de que el administrador se
 * acordara de subirsela.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MiCredencialServiceImplTest {

    private static final String FOTO_PROPIA =
            ApiEndpoint.PUBLICO_FOTOS + "/3f2504e0-4f89-11d3-9a0c-0305e82c3301.jpg";

    @Mock private GdPersonaRepository personaRepository;
    @Mock private GdCredencialQrRepository credencialRepository;
    @Mock private GdResidenteCasaRepository residenteCasaRepository;
    @Mock private CredencialQrService credencialQrService;

    @InjectMocks
    private MiCredencialServiceImpl servicio;

    private GdPersona persona;
    private UsuarioAutenticado residente;

    @BeforeEach
    void preparar() {
        persona = new GdPersona();
        persona.setId(40L);
        persona.setDocumento("1075");
        persona.setNombres("Tomas");
        persona.setApellidos("Carrillo");
        persona.setActivo(Codigos.SI);

        residente = new UsuarioAutenticado(7L, 40L, 1L, "1075", "Tomas Carrillo",
                Codigos.ROL_RESIDENTE);

        lenient().when(personaRepository.findById(40L)).thenReturn(Optional.of(persona));
        lenient().when(personaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(any(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(credencialQrService.emitirPermanente(any(), anyString()))
                .thenReturn(new GdCredencialQr());
        lenient().when(credencialQrService.construirPayload(any())).thenReturn("GRD1.uuid.firma");
    }

    @Test
    @DisplayName("sin foto NO revienta: devuelve el estado 'necesita foto' y sin codigo")
    void sinFotoDevuelveEstadoNoError() {
        MiQrResponse ficha = servicio.miQr(residente);

        assertThat(ficha.isNecesitaFoto()).isTrue();
        assertThat(ficha.getPayload()).isNull();
        // El nombre y el documento SI viajan: la pantalla saluda a la persona
        // aunque todavia no tenga codigo.
        assertThat(ficha.getNombreCompleto()).isEqualTo("Tomas Carrillo");

        // Y no se intenta emitir nada, que es lo que antes lanzaba el 400.
        verify(credencialQrService, never()).emitirPermanente(any(), anyString());
    }

    @Test
    @DisplayName("el residente sube su foto y su credencial queda emitida en la misma respuesta")
    void subirLaFotoEmiteLaCredencial() {
        MiQrResponse ficha = servicio.fijarMiFoto(residente, FOTO_PROPIA);

        assertThat(persona.getFotoUrl()).isEqualTo(FOTO_PROPIA);
        assertThat(ficha.isNecesitaFoto()).isFalse();
        assertThat(ficha.getPayload()).isEqualTo("GRD1.uuid.firma");
        verify(credencialQrService).emitirPermanente(any(), anyString());
    }

    @Test
    @DisplayName("una URL que no salio de la aplicacion se rechaza")
    void rechazaFotoExterna() {
        // El control central del sistema es lo que el guardia compara contra
        // la cara. Aceptar una URL cualquiera deja colar cualquier imagen.
        assertThatThrownBy(() -> servicio.fijarMiFoto(residente, "https://otro-sitio.com/cara.jpg"))
                .isInstanceOf(GuardianException.class)
                .hasMessage(MensajesGlobales.FOTO_URL_INVALIDA);

        assertThat(persona.getFotoUrl()).isNull();
        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("una foto vacia tambien se rechaza: no sirve para desbloquear nada")
    void rechazaFotoVacia() {
        assertThatThrownBy(() -> servicio.fijarMiFoto(residente, "   "))
                .hasMessage(MensajesGlobales.FOTO_URL_INVALIDA);

        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("quien YA tiene credencial la recibe aunque le falte la foto")
    void conCredencialPreviaNoPideFoto() {
        // La foto pudo borrarse despues de emitir el QR. Quitarle el codigo a
        // alguien que ya lo usaba lo dejaria en la puerta sin aviso.
        lenient().when(credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(any(), anyString(), anyString()))
                .thenReturn(Optional.of(new GdCredencialQr()));

        MiQrResponse ficha = servicio.miQr(residente);

        assertThat(ficha.isNecesitaFoto()).isFalse();
        assertThat(ficha.getPayload()).isEqualTo("GRD1.uuid.firma");
    }
}
