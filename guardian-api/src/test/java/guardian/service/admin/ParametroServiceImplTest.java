package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.dto.admin.ParametroRequest;
import guardian.dto.common.ParametroResponse;
import guardian.entity.parametro.GdParametro;
import guardian.exception.GuardianException;
import guardian.repository.GdParametroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las barandas del catalogo administrable.
 *
 * <p>Que el administrador pueda agregar marcas de carro es inofensivo. Que
 * pueda borrar TITULAR, o inventarse un rol, o un motivo de denegacion que el
 * codigo no conoce, no lo es: la logica ramifica sobre esos codigos y no tiene
 * como enterarse de que desaparecieron.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParametroServiceImplTest {

    @Mock
    private GdParametroRepository parametroRepository;

    @Mock
    private EtiquetaCatalogoService etiquetaCatalogoService;

    @InjectMocks
    private ParametroServiceImpl servicio;

    // ── Lo que se puede ampliar y lo que no ──────────────────────────────────

    @Test
    @DisplayName("agrega una marca nueva derivando el codigo del nombre")
    void agregaMarca() {
        when(parametroRepository.findByGrupoAndCodigo(Codigos.GRUPO_MARCA_VEHICULO, "CITROEN"))
                .thenReturn(Optional.empty());
        when(parametroRepository.findByGrupoOrderByOrdenAsc(Codigos.GRUPO_MARCA_VEHICULO))
                .thenReturn(Collections.singletonList(parametro(1L, "MAZDA", "Mazda", 7)));
        when(parametroRepository.save(any(GdParametro.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        ParametroResponse creada = servicio.crear(
                Codigos.GRUPO_MARCA_VEHICULO, solicitud("  Citroën "));

        assertThat(creada.getCodigo()).isEqualTo("CITROEN");
        assertThat(creada.getValor()).isEqualTo("Citroën");
        // Al final de la lista: lo nuevo no se cuela entre lo ya ordenado.
        assertThat(creada.getOrden()).isEqualTo(8);
        assertThat(creada.isProtegido()).isFalse();
    }

    @Test
    @DisplayName("no deja agregar opciones a un grupo estructural")
    void noAmpliaGrupoEstructural() {
        // Un motivo de denegacion nuevo no lo va a emitir nadie: el codigo
        // decide cual se usa. Ofrecerlo seria mentirle al administrador.
        assertThatThrownBy(() -> servicio.crear(
                Codigos.GRUPO_MOTIVO_DENEGACION, solicitud("Se me antojo")))
                .isInstanceOf(GuardianException.class);

        verify(parametroRepository, never()).save(any());
    }

    @Test
    @DisplayName("un grupo desconocido no es administrable ni aunque exista en la tabla")
    void grupoDesconocidoNoSeAdministra() {
        assertThatThrownBy(() -> servicio.administrarGrupo("LO_QUE_SEA"))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("un nombre sin letras ni numeros no alcanza para un codigo")
    void rechazaNombreSinContenido() {
        assertThatThrownBy(() -> servicio.crear(
                Codigos.GRUPO_MARCA_VEHICULO, solicitud("--- ***")))
                .isInstanceOf(GuardianException.class);
    }

    // ── Duplicados y reencendido ─────────────────────────────────────────────

    @Test
    @DisplayName("escribir de nuevo una opcion apagada la reenciende en vez de duplicarla")
    void reenciendeEnVezDeDuplicar() {
        GdParametro apagada = parametro(9L, "MAZDA", "Mazda", 3);
        apagada.setActivo(Codigos.NO);
        when(parametroRepository.findByGrupoAndCodigo(Codigos.GRUPO_MARCA_VEHICULO, "MAZDA"))
                .thenReturn(Optional.of(apagada));
        when(parametroRepository.save(any(GdParametro.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        ParametroResponse resultado = servicio.crear(
                Codigos.GRUPO_MARCA_VEHICULO, solicitud("mazda"));

        // Misma fila, no una segunda: la unicidad (grupo, codigo) lo impediria
        // y el select terminaria con dos "Mazda".
        assertThat(resultado.getId()).isEqualTo(9L);
        assertThat(resultado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("rechaza una opcion que ya existe y esta activa")
    void rechazaDuplicadaActiva() {
        when(parametroRepository.findByGrupoAndCodigo(Codigos.GRUPO_MARCA_VEHICULO, "MAZDA"))
                .thenReturn(Optional.of(parametro(9L, "MAZDA", "Mazda", 3)));

        assertThatThrownBy(() -> servicio.crear(
                Codigos.GRUPO_MARCA_VEHICULO, solicitud("Mazda")))
                .isInstanceOf(GuardianException.class);
    }

    // ── Lo protegido ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("una opcion protegida se renombra pero no se apaga")
    void protegidaSeRenombraNoSeApaga() {
        GdParametro titular = parametro(4L, Codigos.PARENTESCO_TITULAR, "Titular", 1);
        titular.setGrupo(Codigos.GRUPO_PARENTESCO);
        titular.setProtegido(Codigos.SI);
        when(parametroRepository.findById(4L)).thenReturn(Optional.of(titular));
        when(parametroRepository.save(any(GdParametro.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        // Renombrar es inofensivo: el codigo TITULAR sigue ahi.
        ParametroResponse renombrada = servicio.renombrar(4L, solicitud("Titular del hogar"));
        assertThat(renombrada.getValor()).isEqualTo("Titular del hogar");
        assertThat(renombrada.getCodigo()).isEqualTo(Codigos.PARENTESCO_TITULAR);

        // Apagarla dejaria la validacion de titular unico sin nada que buscar.
        assertThatThrownBy(() -> servicio.cambiarEstado(4L, false))
                .isInstanceOf(GuardianException.class);
    }

    @Test
    @DisplayName("apagar una opcion normal no la borra, la oculta")
    void apagarNoBorra() {
        GdParametro marca = parametro(11L, "SUZUKI", "Suzuki", 5);
        marca.setGrupo(Codigos.GRUPO_MARCA_VEHICULO);
        when(parametroRepository.findById(11L)).thenReturn(Optional.of(marca));
        when(parametroRepository.save(any(GdParametro.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        ParametroResponse apagada = servicio.cambiarEstado(11L, false);

        assertThat(apagada.isActivo()).isFalse();
        verify(parametroRepository, never()).delete(any());
    }

    @Test
    @DisplayName("editar el catalogo invalida las etiquetas en cache")
    void invalidaCacheAlEditar() {
        // Sin esto, la ficha de la garita seguiria mostrando el nombre viejo
        // hasta que el cache venciera solo.
        GdParametro marca = parametro(11L, "SUZUKI", "Suzuki", 5);
        marca.setGrupo(Codigos.GRUPO_MARCA_VEHICULO);
        when(parametroRepository.findById(11L)).thenReturn(Optional.of(marca));
        when(parametroRepository.save(any(GdParametro.class)))
                .thenAnswer(llamada -> llamada.getArgument(0));

        servicio.renombrar(11L, solicitud("Suzuki Motor"));

        verify(etiquetaCatalogoService).invalidar(Codigos.GRUPO_MARCA_VEHICULO);
    }

    // ── Ayudas ───────────────────────────────────────────────────────────────

    private ParametroRequest solicitud(String valor) {
        ParametroRequest solicitud = new ParametroRequest();
        solicitud.setValor(valor);
        return solicitud;
    }

    private GdParametro parametro(Long id, String codigo, String valor, int orden) {
        GdParametro parametro = new GdParametro();
        parametro.setId(id);
        parametro.setGrupo(Codigos.GRUPO_MARCA_VEHICULO);
        parametro.setCodigo(codigo);
        parametro.setValor(valor);
        parametro.setOrden(orden);
        parametro.setProtegido(Codigos.NO);
        parametro.setActivo(Codigos.SI);
        return parametro;
    }
}
