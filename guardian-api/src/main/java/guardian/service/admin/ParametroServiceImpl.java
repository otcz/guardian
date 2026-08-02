package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.dto.admin.GrupoParametroResponse;
import guardian.dto.admin.ParametroRequest;
import guardian.dto.common.ParametroResponse;
import guardian.entity.parametro.GdParametro;
import guardian.exception.GuardianException;
import guardian.repository.GdParametroRepository;
import guardian.util.CodigoCatalogoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParametroServiceImpl implements ParametroService {

    /**
     * Como se le presenta cada grupo al administrador. El nombre tecnico
     * —MOTIVO_DENEGACION— no es lo que quiere leer quien entra a Configuracion.
     */
    private static final Map<String, String[]> CATALOGO = catalogo();

    private static Map<String, String[]> catalogo() {
        Map<String, String[]> mapa = new LinkedHashMap<>();
        mapa.put(Codigos.GRUPO_TIPO_VEHICULO, new String[]{
                "Tipos de vehiculo",
                "Que se declara al registrar un vehiculo del hogar."});
        mapa.put(Codigos.GRUPO_MARCA_VEHICULO, new String[]{
                "Marcas de vehiculo",
                "Agrega las que circulen en el conjunto y no esten en la lista."});
        mapa.put(Codigos.GRUPO_COLOR_VEHICULO, new String[]{
                "Colores de vehiculo",
                "Colores basicos: de noche no se distinguen los tonos."});
        mapa.put(Codigos.GRUPO_PARENTESCO, new String[]{
                "Parentescos",
                "El vinculo de cada persona con el titular de la casa."});
        mapa.put(Codigos.GRUPO_TIPO_DOCUMENTO, new String[]{
                "Tipos de documento",
                "Documentos con los que se identifica una persona."});
        mapa.put(Codigos.GRUPO_MOTIVO_DENEGACION, new String[]{
                "Motivos de denegacion",
                "Por que la porteria niega un ingreso. Solo se renombran."});
        mapa.put(Codigos.GRUPO_TIPO_CREDENCIAL, new String[]{
                "Tipos de credencial",
                "Permanente para residentes, temporal para invitados."});
        return mapa;
    }

    private final GdParametroRepository parametroRepository;
    private final EtiquetaCatalogoService etiquetaCatalogoService;

    @Override
    @Transactional(readOnly = true)
    public List<ParametroResponse> listarPorGrupo(String grupo) {
        return parametroRepository
                .findByGrupoAndActivoOrderByOrdenAsc(grupo, Codigos.SI)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void exigirCodigoValido(String grupo, String codigo) {
        if (codigo == null || !parametroRepository
                .existsByGrupoAndCodigoAndActivo(grupo, codigo, Codigos.SI)) {
            throw GuardianException.solicitudInvalida(
                    "El valor seleccionado para " + grupo.toLowerCase() + " no es valido.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrupoParametroResponse> listarGrupos() {
        return CATALOGO.entrySet().stream()
                .map(entrada -> GrupoParametroResponse.builder()
                        .grupo(entrada.getKey())
                        .nombre(entrada.getValue()[0])
                        .descripcion(entrada.getValue()[1])
                        .opciones(parametroRepository
                                .countByGrupoAndActivo(entrada.getKey(), Codigos.SI))
                        .ampliable(esAmpliable(entrada.getKey()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroResponse> administrarGrupo(String grupo) {
        exigirGrupoConocido(grupo);
        return parametroRepository.findByGrupoOrderByOrdenAsc(grupo)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParametroResponse crear(String grupo, ParametroRequest solicitud) {
        exigirGrupoConocido(grupo);
        if (!esAmpliable(grupo)) {
            throw GuardianException.solicitudInvalida(
                    "Este grupo no admite opciones nuevas. Solo puedes renombrar las que existen.");
        }

        String valor = solicitud.getValor().trim();
        String codigo = CodigoCatalogoUtil.desde(valor);
        if (codigo.isEmpty()) {
            throw GuardianException.solicitudInvalida(
                    "El nombre debe tener al menos una letra o un numero.");
        }

        // Reencender antes que duplicar: si "Mazda" se apago y el administrador
        // la vuelve a escribir, esta pidiendo la misma opcion de vuelta. Crear
        // otra fila dejaria la unicidad rota y dos "Mazda" en el select.
        GdParametro existente = parametroRepository.findByGrupoAndCodigo(grupo, codigo)
                .orElse(null);
        if (existente != null) {
            if (existente.estaActivo()) {
                throw GuardianException.conflicto("Ya existe una opcion con ese nombre.");
            }
            existente.setValor(valor);
            existente.setActivo(Codigos.SI);
            etiquetaCatalogoService.invalidar(grupo);
            log.info("[parametro] reactivado grupo={} codigo={}", grupo, codigo);
            return mapear(parametroRepository.save(existente));
        }

        GdParametro parametro = new GdParametro();
        parametro.setGrupo(grupo);
        parametro.setCodigo(codigo);
        parametro.setValor(valor);
        parametro.setOrden(siguienteOrden(grupo));
        parametro.setProtegido(Codigos.NO);
        parametro.setActivo(Codigos.SI);

        etiquetaCatalogoService.invalidar(grupo);
        log.info("[parametro] creado grupo={} codigo={}", grupo, codigo);
        return mapear(parametroRepository.save(parametro));
    }

    @Override
    @Transactional
    public ParametroResponse renombrar(Long id, ParametroRequest solicitud) {
        GdParametro parametro = obtener(id);
        parametro.setValor(solicitud.getValor().trim());
        etiquetaCatalogoService.invalidar(parametro.getGrupo());
        log.info("[parametro] renombrado id={} grupo={} codigo={}",
                id, parametro.getGrupo(), parametro.getCodigo());
        return mapear(parametroRepository.save(parametro));
    }

    @Override
    @Transactional
    public ParametroResponse cambiarEstado(Long id, boolean activo) {
        GdParametro parametro = obtener(id);

        // Los protegidos son los que la logica referencia por codigo: sin
        // TITULAR la validacion de titular unico se queda sin nada que buscar.
        if (!activo && Codigos.SI.equals(parametro.getProtegido())) {
            throw GuardianException.solicitudInvalida(
                    "Esta opcion la usa el sistema y no se puede desactivar. Puedes cambiarle el nombre.");
        }
        if (!activo && !esAmpliable(parametro.getGrupo())) {
            throw GuardianException.solicitudInvalida(
                    "Este grupo no admite quitar opciones. Solo puedes renombrar las que existen.");
        }

        parametro.setActivo(activo ? Codigos.SI : Codigos.NO);
        etiquetaCatalogoService.invalidar(parametro.getGrupo());
        log.info("[parametro] estado id={} grupo={} codigo={} activo={}",
                id, parametro.getGrupo(), parametro.getCodigo(), activo);
        return mapear(parametroRepository.save(parametro));
    }

    private GdParametro obtener(Long id) {
        GdParametro parametro = parametroRepository.findById(id)
                .orElseThrow(() -> GuardianException.noEncontrado("La opcion no existe."));
        exigirGrupoConocido(parametro.getGrupo());
        return parametro;
    }

    /**
     * Un grupo que no esta en el catalogo no se administra. Sin esto, la URL
     * seria un CRUD abierto sobre cualquier fila de GD_PARAMETRO.
     */
    private void exigirGrupoConocido(String grupo) {
        if (grupo == null || !CATALOGO.containsKey(grupo)) {
            throw GuardianException.noEncontrado("Ese grupo de configuracion no existe.");
        }
    }

    private boolean esAmpliable(String grupo) {
        return Codigos.GRUPOS_ABIERTOS.contains(grupo);
    }

    /** Al final de la lista: lo nuevo no se cuela entre lo que ya estaba ordenado. */
    private int siguienteOrden(String grupo) {
        return parametroRepository.findByGrupoOrderByOrdenAsc(grupo).stream()
                .map(GdParametro::getOrden)
                .filter(orden -> orden != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private ParametroResponse mapear(GdParametro parametro) {
        return ParametroResponse.builder()
                .id(parametro.getId())
                .grupo(parametro.getGrupo())
                .codigo(parametro.getCodigo())
                .valor(parametro.getValor())
                .orden(parametro.getOrden())
                .protegido(Codigos.SI.equals(parametro.getProtegido()))
                .activo(parametro.estaActivo())
                .build();
    }
}
