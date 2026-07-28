package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.persona.GdPersona;
import guardian.exception.GuardianException;
import guardian.repository.GdCredencialQrRepository;
import guardian.util.HmacUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas del QR: emision, formato del payload y rechazo de codigos alterados.
 */
@ExtendWith(MockitoExtension.class)
class CredencialQrServiceImplTest {

    private static final String SECRETO = "secreto-de-pruebas-de-32-bytes!!!!";
    private static final String CODIGO = "ca687098-52af-45e1-8706-e198e98c5b9c";

    @Mock
    private GdCredencialQrRepository credencialRepository;

    @InjectMocks
    private CredencialQrServiceImpl service;

    private GdCredencialQr credencial;

    @BeforeEach
    void prepararSecreto() {
        ReflectionTestUtils.setField(service, "secretoHmac", SECRETO);

        credencial = new GdCredencialQr();
        credencial.setId(1L);
        credencial.setCodigoPublico(CODIGO);
        credencial.setFirmaHash(HmacUtil.firmar(CODIGO, SECRETO));
        credencial.setTipo(Codigos.CREDENCIAL_PERMANENTE);
        credencial.setUsosRealizados(0);
        credencial.setActivo(Codigos.SI);
    }

    // ── Emision ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("no se emite credencial a una persona sin foto")
    void exigeFoto() {
        // La foto es lo que deja al guardia confirmar que quien muestra el QR es
        // su dueno. Sin ella la credencial seria un pase transferible.
        GdPersona sinFoto = new GdPersona();
        sinFoto.setId(7L);

        assertThatThrownBy(() -> service.emitirPermanente(sinFoto, "admin"))
                .isInstanceOf(GuardianException.class)
                .hasMessageContaining("foto");

        verify(credencialRepository, never()).save(any());
    }

    @Test
    @DisplayName("una foto en blanco no cuenta como foto")
    void fotoEnBlancoNoCuenta() {
        GdPersona persona = new GdPersona();
        persona.setId(7L);
        persona.setFotoUrl("   ");

        assertThatThrownBy(() -> service.emitirPermanente(persona, "admin"))
                .isInstanceOf(GuardianException.class);
    }

    // ── Payload ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("el payload lleva prefijo con version, codigo y firma")
    void formatoDelPayload() {
        String payload = service.construirPayload(credencial);

        assertThat(payload.split("\\.")).hasSize(3);
        assertThat(payload).startsWith("GRD1.").contains(CODIGO);
    }

    @Test
    @DisplayName("el payload no lleva datos personales")
    void payloadSinDatosPersonales() {
        // Fotografiar el QR de un vecino no debe entregar informacion util.
        GdPersona persona = new GdPersona();
        persona.setDocumento("52123456");
        persona.setNombres("Maria");
        persona.setApellidos("Gomez");
        credencial.setPersona(persona);

        assertThat(service.construirPayload(credencial))
                .doesNotContain("52123456", "Maria", "Gomez");
    }

    // ── Resolucion ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("un payload valido resuelve a su credencial")
    void resuelvePayloadValido() {
        when(credencialRepository.buscarPorCodigoPublico(CODIGO))
                .thenReturn(Optional.of(credencial));

        Optional<GdCredencialQr> resuelta = service.resolver(service.construirPayload(credencial));

        assertThat(resuelta).containsSame(credencial);
    }

    @Test
    @DisplayName("una firma alterada se rechaza sin llegar a la base")
    void rechazaFirmaAlterada() {
        String falso = "GRD1." + CODIGO + ".firmaInventadaQueNoCuadra";

        assertThat(service.resolver(falso)).isEmpty();

        // Sin este corte, un ataque de fuerza bruta contra la porteria podria
        // saturar la base de datos a punta de codigos inventados.
        verify(credencialRepository, never()).buscarPorCodigoPublico(anyString());
    }

    @Test
    @DisplayName("se rechazan payloads mal formados")
    void rechazaPayloadsMalFormados() {
        assertThat(service.resolver(null)).isEmpty();
        assertThat(service.resolver("")).isEmpty();
        assertThat(service.resolver("cualquier-cosa")).isEmpty();
        assertThat(service.resolver("GRD1." + CODIGO)).isEmpty();
        assertThat(service.resolver("GRD1." + CODIGO + ".firma.sobra")).isEmpty();

        verify(credencialRepository, never()).buscarPorCodigoPublico(anyString());
    }

    @Test
    @DisplayName("se rechaza un prefijo de version desconocido")
    void rechazaPrefijoDesconocido() {
        String otraVersion = "GRD9." + CODIGO + "." + HmacUtil.firmar(CODIGO, SECRETO);

        assertThat(service.resolver(otraVersion)).isEmpty();
    }

    @Test
    @DisplayName("un QR firmado con otro secreto no valida")
    void rechazaOtroSecreto() {
        String ajeno = "GRD1." + CODIGO + "." + HmacUtil.firmar(CODIGO, "secreto-ajeno-de-32-bytes!!!!!!!!");

        assertThat(service.resolver(ajeno)).isEmpty();
    }
}
