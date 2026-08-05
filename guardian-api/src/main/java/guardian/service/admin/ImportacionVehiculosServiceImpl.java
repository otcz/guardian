package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.ImportacionVehiculosResponse;
import guardian.dto.admin.VehiculoRequest;
import guardian.dto.common.ParametroResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.exception.GuardianException;
import guardian.repository.GdCasaRepository;
import guardian.security.UsuarioAutenticado;
import guardian.util.CeldaExcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportacionVehiculosServiceImpl implements ImportacionVehiculosService {

    /** Un conjunto no tiene 5000 vehiculos; un archivo con 5000 es un error. */
    private static final int MAXIMO_FILAS = 5000;

    private final VehiculoService vehiculoService;
    private final ParametroService parametroService;
    private final GdCasaRepository casaRepository;

    @Override
    public ImportacionVehiculosResponse importar(MultipartFile archivo,
                                                 UsuarioAutenticado ejecutor) {
        exigirArchivo(archivo);

        List<ImportacionVehiculosResponse.FilaRechazada> rechazos = new ArrayList<>();
        int leidas = 0;
        int creadas = 0;
        int repetidas = 0;

        List<String> tipos = codigos(Codigos.GRUPO_TIPO_VEHICULO);
        List<String> marcas = codigos(Codigos.GRUPO_MARCA_VEHICULO);
        List<String> colores = codigos(Codigos.GRUPO_COLOR_VEHICULO);

        // Sin withCellFormat: aqui no hay fechas, y pedirlo obliga al lector a
        // exigir styles.xml — hay generadores de xlsx que no lo escriben.
        try (InputStream entrada = archivo.getInputStream();
             ReadableWorkbook libro = new ReadableWorkbook(entrada,
                     new ReadingOptions(false, true))) {

            Sheet hoja = libro.getFirstSheet();
            try (Stream<Row> filas = hoja.openStream()) {
                for (Row fila : filas.collect(Collectors.toList())) {

                    int numeroFila = fila.getRowNum();
                    if (numeroFila == 1) {
                        continue;
                    }

                    String casa = CeldaExcel.texto(fila, 0);
                    String placa = CeldaExcel.texto(fila, 1);
                    String tipo = CeldaExcel.texto(fila, 2);
                    String marca = CeldaExcel.texto(fila, 3);
                    String color = CeldaExcel.texto(fila, 4);

                    // Fila del todo vacia: es el final del archivo o una linea
                    // que quedo al borrar contenido, no un error que reportar.
                    if (casa.isEmpty() && placa.isEmpty() && tipo.isEmpty()
                            && marca.isEmpty() && color.isEmpty()) {
                        continue;
                    }

                    leidas++;
                    if (leidas > MAXIMO_FILAS) {
                        rechazos.add(rechazo(numeroFila, placa, casa,
                                "El archivo supera las " + MAXIMO_FILAS + " filas."));
                        break;
                    }

                    if (placa.isEmpty()) {
                        rechazos.add(rechazo(numeroFila, placa, casa, "Falta la placa."));
                        continue;
                    }

                    Optional<GdCasa> casaEncontrada = buscarCasa(casa, ejecutor.getConjuntoId());
                    if (!casaEncontrada.isPresent()) {
                        rechazos.add(rechazo(numeroFila, placa, casa, casa.isEmpty()
                                ? "Falta la casa."
                                : "No hay ninguna casa con ese nombre. Registrala primero."));
                        continue;
                    }

                    String codigoTipo = resolver(tipo, tipos);
                    if (codigoTipo == null) {
                        rechazos.add(rechazo(numeroFila, placa, casa,
                                "Tipo no valido. Usa: " + String.join(", ", tipos)));
                        continue;
                    }

                    // Marca y color SI son opcionales, pero si se escribe algo
                    // tiene que estar en el catalogo: una marca inventada no se
                    // puede filtrar despues ni agrupar en un reporte.
                    String codigoMarca = marca.isEmpty() ? null : resolver(marca, marcas);
                    if (!marca.isEmpty() && codigoMarca == null) {
                        rechazos.add(rechazo(numeroFila, placa, casa,
                                "Marca no valida. Agregala en Configuracion o dejala vacia."));
                        continue;
                    }
                    String codigoColor = color.isEmpty() ? null : resolver(color, colores);
                    if (!color.isEmpty() && codigoColor == null) {
                        rechazos.add(rechazo(numeroFila, placa, casa,
                                "Color no valido. Agregalo en Configuracion o dejalo vacio."));
                        continue;
                    }

                    VehiculoRequest peticion = new VehiculoRequest();
                    peticion.setCasaId(casaEncontrada.get().getId());
                    peticion.setPlaca(placa);
                    peticion.setTipo(codigoTipo);
                    peticion.setMarca(codigoMarca);
                    peticion.setColor(codigoColor);

                    try {
                        // Por el SERVICE y no con un insert directo: la carga
                        // masiva pasa por las mismas reglas que el alta de a una
                        // —placa unica en todo el sistema, sede, auditoria— y no
                        // puede divergir de ella.
                        vehiculoService.crear(peticion, ejecutor);
                        creadas++;
                    } catch (GuardianException fallo) {
                        if (MensajesGlobales.PLACA_YA_REGISTRADA.equals(fallo.getMessage())) {
                            repetidas++;
                        }
                        rechazos.add(rechazo(numeroFila, placa, casa, fallo.getMessage()));
                    }
                }
            }
        } catch (IOException | RuntimeException fallo) {
            log.warn("[admin] no se pudo leer el Excel de vehiculos: {}", fallo.toString());
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_ILEGIBLE);
        }

        log.info("[admin] importacion de vehiculos: leidas={} creadas={} repetidas={} errores={} por={}",
                leidas, creadas, repetidas, rechazos.size() - repetidas, ejecutor.getDocumento());

        return ImportacionVehiculosResponse.builder()
                .leidas(leidas)
                .creadas(creadas)
                .repetidas(repetidas)
                .conError(rechazos.size() - repetidas)
                .rechazos(rechazos)
                .build();
    }

    @Override
    public byte[] plantilla(Long conjuntoId) {
        List<String> tipos = codigos(Codigos.GRUPO_TIPO_VEHICULO);
        List<String> marcas = codigos(Codigos.GRUPO_MARCA_VEHICULO);
        List<String> colores = codigos(Codigos.GRUPO_COLOR_VEHICULO);
        List<GdCasa> casas = casaRepository.findByConjuntoIdOrderByIdentificadorAsc(conjuntoId);

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Workbook libro = new Workbook(salida, "GUARDIAN", "1.0");

            Worksheet hoja = libro.newWorksheet("Vehiculos");
            String[] columnas = {COLUMNA_CASA, COLUMNA_PLACA, COLUMNA_TIPO,
                    COLUMNA_MARCA, COLUMNA_COLOR};
            for (int i = 0; i < columnas.length; i++) {
                hoja.value(0, i, columnas[i]);
                hoja.style(0, i).bold().set();
            }

            // Ejemplo con datos REALES de la sede: una plantilla que muestra una
            // casa o una marca que este conjunto no tiene ensena a fallar.
            hoja.value(1, 0, casas.isEmpty() ? "CASA-101" : casas.get(0).getIdentificador());
            hoja.value(1, 1, "ABC123");
            hoja.value(1, 2, primero(tipos, "CARRO"));
            hoja.value(1, 3, primero(marcas, "RENAULT"));
            hoja.value(1, 4, primero(colores, "BLANCO"));

            hoja.width(0, 18);
            hoja.width(1, 14);
            hoja.width(2, 16);
            hoja.width(3, 18);
            hoja.width(4, 16);

            // Segunda hoja con lo que se puede escribir. Sin ella el
            // administrador tiene que adivinar los codigos o volver a la
            // pantalla a mirarlos uno por uno.
            Worksheet valores = libro.newWorksheet("Valores validos");
            valores.value(0, 0, "Casas");
            valores.value(0, 1, COLUMNA_TIPO);
            valores.value(0, 2, COLUMNA_MARCA);
            valores.value(0, 3, COLUMNA_COLOR);
            for (int i = 0; i < 4; i++) {
                valores.style(0, i).bold().set();
                valores.width(i, 20);
            }
            escribirColumna(valores, 0, casas.stream()
                    .map(GdCasa::getIdentificador).collect(Collectors.toList()));
            escribirColumna(valores, 1, tipos);
            escribirColumna(valores, 2, marcas);
            escribirColumna(valores, 3, colores);

            libro.finish();
            return salida.toByteArray();
        } catch (IOException fallo) {
            log.error("[admin] no se pudo generar la plantilla de vehiculos", fallo);
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_ILEGIBLE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void escribirColumna(Worksheet hoja, int columna, List<String> valores) {
        for (int i = 0; i < valores.size(); i++) {
            hoja.value(i + 1, columna, valores.get(i));
        }
    }

    private String primero(List<String> valores, String respaldo) {
        return valores.isEmpty() ? respaldo : valores.get(0);
    }

    private List<String> codigos(String grupo) {
        return parametroService.listarPorGrupo(grupo).stream()
                .map(ParametroResponse::getCodigo)
                .collect(Collectors.toList());
    }

    /**
     * La casa por su nombre, ignorando mayusculas y espacios: el administrador
     * escribe "casa-101" o "CASA-101" y las dos tienen que servir.
     */
    private Optional<GdCasa> buscarCasa(String identificador, Long conjuntoId) {
        String limpio = identificador.trim();
        if (limpio.isEmpty()) {
            return Optional.empty();
        }
        return casaRepository.findByConjuntoIdOrderByIdentificadorAsc(conjuntoId).stream()
                .filter(casa -> casa.getIdentificador().equalsIgnoreCase(limpio))
                .findFirst();
    }

    /** Acepta el codigo sin importar mayusculas: "carro" y "CARRO" son lo mismo. */
    private String resolver(String escrito, List<String> validos) {
        String limpio = escrito.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return validos.stream()
                .filter(codigo -> codigo.equalsIgnoreCase(limpio))
                .findFirst()
                .orElse(null);
    }

    private void exigirArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_VACIO);
        }
        String nombre = Optional.ofNullable(archivo.getOriginalFilename()).orElse("")
                .toLowerCase(Locale.ROOT);
        if (!nombre.endsWith(".xlsx")) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_FORMATO);
        }
    }

    private ImportacionVehiculosResponse.FilaRechazada rechazo(int fila, String placa,
                                                               String casa, String motivo) {
        return ImportacionVehiculosResponse.FilaRechazada.builder()
                .fila(fila)
                .placa(placa)
                .casa(casa)
                .motivo(motivo)
                .build();
    }
}
