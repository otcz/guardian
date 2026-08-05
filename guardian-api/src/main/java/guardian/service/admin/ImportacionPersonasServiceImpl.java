package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.ImportacionPersonasResponse;
import guardian.dto.admin.PersonaRequest;
import guardian.exception.GuardianException;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportacionPersonasServiceImpl implements ImportacionPersonasService {

    /** Un conjunto no tiene 5000 residentes; un archivo con 5000 es un error. */
    private static final int MAXIMO_FILAS = 5000;

    /** El mismo criterio flexible de @Email: algo, arroba, algo, punto, algo. */
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PersonaService personaService;

    /**
     * Se lee CON los formatos de celda y, si eso falla, otra vez sin ellos.
     *
     * <p>Los formatos hacen falta para las fechas: Excel las guarda como el
     * numero de dias desde 1900, y sin el formato son indistinguibles de una
     * cedula. Pero pedirlos obliga al lector a exigir styles.xml, y hay
     * generadores de xlsx que no lo escriben. El segundo intento salva esos
     * archivos — sus fechas solo se entenderan si vienen como texto, que es
     * mejor que un "no pudimos leer el archivo" sin salida.</p>
     */
    @Override
    public ImportacionPersonasResponse importar(MultipartFile archivo,
                                                UsuarioAutenticado ejecutor) {
        exigirArchivo(archivo);
        try {
            return leer(archivo, new ReadingOptions(true, true), ejecutor);
        } catch (GuardianException fallo) {
            if (!MensajesGlobales.EXCEL_ILEGIBLE.equals(fallo.getMessage())) {
                throw fallo;
            }
            log.info("[admin] el Excel no trae formatos de celda; se reintenta sin ellos");
            return leer(archivo, new ReadingOptions(false, true), ejecutor);
        }
    }

    private ImportacionPersonasResponse leer(MultipartFile archivo, ReadingOptions opciones,
                                             UsuarioAutenticado ejecutor) {
        List<ImportacionPersonasResponse.FilaRechazada> rechazos = new ArrayList<>();
        int leidas = 0;
        int creadas = 0;
        int repetidas = 0;

        // cellInErrorIfParseError=true: una celda que no se entiende marca ESA
        // celda, en vez de lanzar y tumbar la lectura del archivo entero.
        try (InputStream entrada = archivo.getInputStream();
             ReadableWorkbook libro = new ReadableWorkbook(entrada, opciones)) {

            Sheet hoja = libro.getFirstSheet();
            try (Stream<Row> filas = hoja.openStream()) {
                for (Row fila : filas.collect(Collectors.toList())) {

                    int numeroFila = fila.getRowNum();
                    if (numeroFila == 1) {
                        continue;
                    }

                    String documento = texto(fila, 0);
                    String nombres = texto(fila, 1);
                    String apellidos = texto(fila, 2);
                    String nacimientoEscrito = texto(fila, 3);
                    LocalDate nacimiento = CeldaExcel.fecha(fila, 3);
                    String correo = texto(fila, 4);
                    String telefono = texto(fila, 5);

                    // Fila del todo vacia: es el final del archivo o una linea
                    // que quedo al borrar contenido, no un error que reportar.
                    if (documento.isEmpty() && nombres.isEmpty() && apellidos.isEmpty()
                            && nacimientoEscrito.isEmpty()
                            && correo.isEmpty() && telefono.isEmpty()) {
                        continue;
                    }

                    leidas++;
                    if (leidas > MAXIMO_FILAS) {
                        rechazos.add(rechazo(numeroFila, documento, nombres,
                                "El archivo supera las " + MAXIMO_FILAS + " filas."));
                        break;
                    }

                    String falta = queFalta(documento, nombres, apellidos, correo);
                    if (falta == null) {
                        falta = revisarNacimiento(nacimientoEscrito, nacimiento);
                    }
                    if (falta != null) {
                        rechazos.add(rechazo(numeroFila, documento, nombres, falta));
                        continue;
                    }

                    PersonaRequest peticion = new PersonaRequest();
                    peticion.setDocumento(documento);
                    peticion.setNombres(nombres);
                    peticion.setApellidos(apellidos);
                    peticion.setEmail(correo);
                    peticion.setTelefono(telefono.isEmpty() ? null : telefono);
                    peticion.setFechaNacimiento(nacimiento == null ? null
                            : Date.from(nacimiento.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    // Residente y sin casa: la carga masiva registra a la gente,
                    // y quien vive donde se asigna despues — meter a alguien en
                    // una casa exige decir tambien si es el titular.
                    peticion.setRolUsuario(Codigos.ROL_RESIDENTE);

                    try {
                        // Por el SERVICE y no con un insert directo: la carga
                        // masiva pasa por las mismas reglas que el alta de a una
                        // —documento repetido, correo, cuenta, auditoria— y no
                        // puede divergir de ella.
                        personaService.crear(peticion, ejecutor);
                        creadas++;
                    } catch (GuardianException fallo) {
                        if (MensajesGlobales.DOCUMENTO_YA_REGISTRADO.equals(fallo.getMessage())) {
                            repetidas++;
                        }
                        rechazos.add(rechazo(numeroFila, documento, nombres, fallo.getMessage()));
                    }
                }
            }
        } catch (IOException | RuntimeException fallo) {
            log.warn("[admin] no se pudo leer el Excel de personas: {}", fallo.toString());
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_ILEGIBLE);
        }

        log.info("[admin] importacion de personas: leidas={} creadas={} repetidas={} errores={} por={}",
                leidas, creadas, repetidas, rechazos.size() - repetidas, ejecutor.getDocumento());

        return ImportacionPersonasResponse.builder()
                .leidas(leidas)
                .creadas(creadas)
                .repetidas(repetidas)
                .conError(rechazos.size() - repetidas)
                .rechazos(rechazos)
                .build();
    }

    @Override
    public byte[] plantilla() {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Workbook libro = new Workbook(salida, "GUARDIAN", "1.0");
            Worksheet hoja = libro.newWorksheet("Personas");

            String[] columnas = {COLUMNA_DOCUMENTO, COLUMNA_NOMBRES, COLUMNA_APELLIDOS,
                    COLUMNA_NACIMIENTO, COLUMNA_CORREO, COLUMNA_TELEFONO};
            for (int i = 0; i < columnas.length; i++) {
                hoja.value(0, i, columnas[i]);
                hoja.style(0, i).bold().set();
            }

            // Una fila de ejemplo con un apellido compuesto: es justo el caso que
            // explica por que Nombres y Apellidos van en columnas separadas.
            hoja.value(1, 0, "1020304050");
            hoja.value(1, 1, "Juan Carlos");
            hoja.value(1, 2, "de la Cruz Perez");
            hoja.value(1, 3, LocalDate.of(1990, 3, 15));
            hoja.value(1, 4, "juan@correo.com");
            hoja.value(1, 5, "3001234567");

            // La columna se marca como fecha: sin formato, Excel muestra el
            // numero de serie —32947— y quien abre la plantilla no entiende que
            // se le esta pidiendo. Es tambien la marca por la que el lector
            // distingue una fecha de un numero cualquiera.
            hoja.style(1, 3).format("dd/mm/yyyy").set();

            hoja.width(0, 18);
            hoja.width(1, 20);
            hoja.width(2, 22);
            hoja.width(3, 20);
            hoja.width(4, 28);
            hoja.width(5, 16);
            libro.finish();
            return salida.toByteArray();
        } catch (IOException fallo) {
            log.error("[admin] no se pudo generar la plantilla de personas", fallo);
            throw GuardianException.solicitudInvalida(MensajesGlobales.EXCEL_ILEGIBLE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lo que falta ANTES de llamar al service. Se valida aca para poder decir
     * QUE columna esta mal en esa fila: el error de Bean Validation llegaria
     * como un mensaje de campo sin numero de fila, y en un archivo de doscientas
     * eso no sirve de nada.
     */
    private String queFalta(String documento, String nombres, String apellidos, String correo) {
        if (documento.isEmpty()) {
            return "Falta la identificacion.";
        }
        if (nombres.isEmpty()) {
            return "Faltan los nombres.";
        }
        if (apellidos.isEmpty()) {
            return "Faltan los apellidos.";
        }
        // El correo es obligatorio porque toda persona se registra CON cuenta, y
        // es por donde recupera su PIN. Sin el, la cuenta nace incomunicada.
        if (correo.isEmpty()) {
            return "Falta el correo: es por donde recupera su PIN.";
        }
        if (!CORREO.matcher(correo).matches()) {
            return "El correo no tiene un formato valido.";
        }
        return null;
    }

    /**
     * La fecha es OPCIONAL, pero si se escribio algo tiene que entenderse: una
     * fecha ilegible que se guarda como nula deja a la persona registrada con
     * un dato que el administrador cree haber puesto.
     */
    private String revisarNacimiento(String escrito, LocalDate fecha) {
        if (escrito.isEmpty()) {
            return null;
        }
        if (fecha == null) {
            return "No entendimos la fecha de nacimiento. Escribela como 15/03/1990.";
        }
        if (!fecha.isBefore(LocalDate.now())) {
            return "La fecha de nacimiento tiene que ser anterior a hoy.";
        }
        return null;
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

    /** Cualquier tipo de celda como texto: Excel guarda las cedulas como numero. */
    private String texto(Row fila, int columna) {
        return CeldaExcel.texto(fila, columna);
    }

    private ImportacionPersonasResponse.FilaRechazada rechazo(int fila, String documento,
                                                              String nombre, String motivo) {
        return ImportacionPersonasResponse.FilaRechazada.builder()
                .fila(fila)
                .documento(documento)
                .nombre(nombre)
                .motivo(motivo)
                .build();
    }
}
