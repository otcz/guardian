package guardian.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacUtilTest {

    private static final String SECRETO = "un-secreto-de-pruebas-de-32-bytes!!";
    private static final String OTRO_SECRETO = "otro-secreto-distinto-de-32-bytes!!";

    @Test
    @DisplayName("la firma es determinista para el mismo contenido y secreto")
    void firmaDeterminista() {
        String a = HmacUtil.firmar("credencial-123", SECRETO);
        String b = HmacUtil.firmar("credencial-123", SECRETO);

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("un contenido distinto produce una firma distinta")
    void contenidoDistintoFirmaDistinta() {
        assertThat(HmacUtil.firmar("credencial-123", SECRETO))
                .isNotEqualTo(HmacUtil.firmar("credencial-124", SECRETO));
    }

    @Test
    @DisplayName("cambiar el secreto invalida las firmas anteriores")
    void secretoDistintoFirmaDistinta() {
        // Este es el caso que documenta el application.properties: rotar
        // GUARDIAN_QR_HMAC_SECRET obliga a reemitir TODAS las credenciales.
        assertThat(HmacUtil.firmar("credencial-123", SECRETO))
                .isNotEqualTo(HmacUtil.firmar("credencial-123", OTRO_SECRETO));
    }

    @Test
    @DisplayName("la firma no expone el secreto en claro")
    void firmaNoExponeSecreto() {
        assertThat(HmacUtil.firmar("credencial-123", SECRETO)).doesNotContain(SECRETO);
    }

    @Test
    @DisplayName("coincide acepta iguales y rechaza distintos, nulos y longitudes diferentes")
    void comparacion() {
        String firma = HmacUtil.firmar("credencial-123", SECRETO);

        assertThat(HmacUtil.coincide(firma, firma)).isTrue();
        assertThat(HmacUtil.coincide(firma, firma + "x")).isFalse();
        assertThat(HmacUtil.coincide(firma, "")).isFalse();
        assertThat(HmacUtil.coincide(firma, null)).isFalse();
        assertThat(HmacUtil.coincide(null, firma)).isFalse();
        assertThat(HmacUtil.coincide(null, null)).isFalse();
    }

    @Test
    @DisplayName("una firma alterada en un solo caracter se rechaza")
    void alteracionMinima() {
        String firma = HmacUtil.firmar("credencial-123", SECRETO);
        char primero = firma.charAt(0);
        String alterada = (primero == 'A' ? 'B' : 'A') + firma.substring(1);

        assertThat(HmacUtil.coincide(firma, alterada)).isFalse();
    }
}
