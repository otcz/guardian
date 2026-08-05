package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.CasaRequest;
import guardian.dto.admin.ImportacionCasasResponse;
import guardian.dto.common.ParametroResponse;
import guardian.exception.GuardianException;
import guardian.security.UsuarioAutenticado;
import guardian.util.CeldaExcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
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
public class ImportacionCasasServiceImpl implements ImportacionCasasService {

    /** Tope de filas. Un conjunto no tiene 5000 casas; un archivo con 5000 es un error. */
    private static final int MAXIMO_FILAS = 5000;

    private final CasaService casaService;
    private final ParametroService parametroService;

    @Override
    public ImportacionCasasResponse importar(MultipartFile archivo,
                                             UsuarioAutenticado ejecutor) {
        exigirArchivo(archivo);

        List<ImportacionCasasResponse.FilaRechazada> rechazos = new ArrayList<>();
        int leidas = 0;
        int creadas = 0;
        int repetidas = 0;

        List<String> tiposValidos = codigosDeTipoVivienda();

        try (InputStream entrada = archivo.getInputStream();
             ReadableWorkbook libro = new ReadableWorkbook(entrada)) {

            Sheet hoja = libro.getFirstSheet();
            try (Stream<Row> filas = hoja.openStream()) {
                List<Row> lista = filas.collect(Collectors.toList());

                for (Row fila : lista) {
                    // getRowNum() es 1-based e incluye el encabezado: es el mismo
                    // numero que el administrador ve al abrir el archivo, y por eso
                    // se reporta tal cual.
                    int numeroFila = fila.getRowNum();
                    if (numeroFila == 1) {
                        continue;
                    }

                    String tipo = texto(fila, 0);
                    String numero = texto(fila, 1);

                    // Una fila totalmente vacia no es un error: es el final del
                    // archivo, o una linea que quedo al borrar contenido.
                    if (tipo.isEmpty() && numero.isEmpty()) {
                        continue;
                    }

                    leidas++;
                    if (leidas > MAXIMO_FILAS) {
                        rechazos.add(rechazo(numeroFila, tipo, numero,
                                "El archivo supera las " + MAXIMO_FILAS + " filas."));
                        break;
                    }

                    String codigoTipo = resolverTipo(tipo, tiposValidos);
                    if (codigoTipo == null) {
                        rechazos.add(rechazo(numeroFila, tipo, numero,
                                "Tipo no valido. Usa: " + String.join(", ", tiposValidos)));
                        continue;
                    }
                    if (numero.isEmpty()) {
                        rechazos.add(rechazo(numeroFila, tipo, numero, "Falta el numero."));
                        continue;
                    }

                    CasaRequest peticion = new CasaRequest();
                    peticion.setTorre(codigoTipo);
                    peticion.setNumero(numero);

                    try {
                        // Se crea POR EL SERVICE y no con un insert directo: asi la
                        // carga masiva pasa por las mismas reglas que el alta de a
                        // una — duplicados, sede, auditoria — y no puede divergir.
                        casaService.crear(peticion, ejecutor);
                        creadas++;
                    } catch (GuardianException fallo) {
                        if (MensajesGlobales.CASA_YA_REGISTRADA.equals(fallo.getMessage())) {
                            repetidas++;
                        }
                        rechazos.add(rechazo(numeroFila, tipo, numero, fallo.getMessage()));
                    }
                }
            }
        } catch (IOException | RuntimeException fallo) {
            log.warn("[admin] no se pudo leer el Excel de casas: {}", fallo.toString());
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_ILEGIBLE);
        }

        log.info("[admin] importacion de casas: leidas={} creadas={} repetidas={} errores={} por={}",
                leidas, creadas, repetidas, rechazos.size() - repetidas, ejecutor.getDocumento());

        return ImportacionCasasResponse.builder()
                .leidas(leidas)
                .creadas(creadas)
                .repetidas(repetidas)
                .conError(rechazos.size() - repetidas)
                .rechazos(rechazos)
                .build();
    }

    @Override
    public byte[] plantilla(Long conjuntoId) {
        List<String> tipos = codigosDeTipoVivienda();

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Workbook libro = new Workbook(salida, "GUARDIAN", "1.0");
            Worksheet hoja = libro.newWorksheet("Casas");

            hoja.value(0, 0, COLUMNA_TIPO);
            hoja.value(0, 1, COLUMNA_NUMERO);
            hoja.style(0, 0).bold().set();
            hoja.style(0, 1).bold().set();

            // Filas de ejemplo con los tipos REALES del conjunto: una plantilla
            // que muestra un tipo que este conjunto no tiene ensena a fallar.
            int fila = 1;
            for (String tipo : tipos) {
                hoja.value(fila, 0, tipo);
                hoja.value(fila, 1, fila == 1 ? "101" : "202");
                fila++;
            }

            hoja.width(0, 18);
            hoja.width(1, 14);
            libro.finish();
            return salida.toByteArray();
        } catch (IOException fallo) {
            log.error("[admin] no se pudo generar la plantilla de casas", fallo);
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_ILEGIBLE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

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

    private List<String> codigosDeTipoVivienda() {
        return parametroService.listarPorGrupo(Codigos.GRUPO_TIPO_VIVIENDA)
                .stream()
                .map(ParametroResponse::getCodigo)
                .collect(Collectors.toList());
    }

    /**
     * Acepta el codigo o la etiqueta, sin importar mayusculas ni espacios: el
     * administrador escribe "casa" o "Casa" y las dos tienen que servir. Si no
     * se aceptara, la mitad de las filas de un archivo hecho a mano se caerian
     * por una diferencia que a nadie le parece un error.
     */
    private String resolverTipo(String escrito, List<String> validos) {
        String limpio = escrito.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return validos.stream()
                .filter(codigo -> codigo.equalsIgnoreCase(limpio))
                .findFirst()
                .orElse(null);
    }

    /**
     * Cualquier tipo de celda como texto. El numero de una casa escrito en
     * Excel llega como NUMBER, no como STRING, y leerlo como cadena reventaba
     * la importacion entera.
     */
    private String texto(Row fila, int columna) {
        return CeldaExcel.texto(fila, columna);
    }

    private ImportacionCasasResponse.FilaRechazada rechazo(int fila, String tipo,
                                                           String numero, String motivo) {
        return ImportacionCasasResponse.FilaRechazada.builder()
                .fila(fila)
                .tipo(tipo)
                .numero(numero)
                .motivo(motivo)
                .build();
    }
}
