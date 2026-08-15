package guardian.service.notificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * El CONTENIDO de un correo, sin nada de presentación.
 *
 * <p>Separa qué se dice de cómo se ve. Los services de notificación redactan
 * con esto y no saben que existe el HTML; {@link PlantillaCorreo} se encarga de
 * vestirlo. Sin esta frontera, cambiar el diseño obligaría a tocar los seis
 * avisos, y cada aviso terminaría con su propia versión del encabezado.</p>
 */
public final class MensajeCorreo {

    private final String asunto;
    private final String titulo;
    private final String saludo;
    private final List<String> parrafos = new ArrayList<>();

    /** Un dato que hay que leer de un vistazo: el código, la placa. Opcional. */
    private String etiquetaDestacado;
    private String destacado;

    /** Aviso en caja aparte, para lo que no se puede resolver solo. Opcional. */
    private String advertencia;

    /** Botón al final. Sin URL configurada no se dibuja. Opcional. */
    private String textoAccion;
    private String urlAccion;

    private MensajeCorreo(String asunto, String titulo, String saludo) {
        this.asunto = asunto;
        this.titulo = titulo;
        this.saludo = saludo;
    }

    public static MensajeCorreo de(String asunto, String titulo, String saludo) {
        return new MensajeCorreo(asunto, titulo, saludo);
    }

    public MensajeCorreo parrafo(String texto) {
        if (texto != null && !texto.trim().isEmpty()) {
            parrafos.add(texto.trim());
        }
        return this;
    }

    public MensajeCorreo destacado(String etiqueta, String valor) {
        this.etiquetaDestacado = etiqueta;
        this.destacado = valor;
        return this;
    }

    public MensajeCorreo advertencia(String texto) {
        this.advertencia = texto;
        return this;
    }

    public MensajeCorreo accion(String texto, String url) {
        if (url != null && !url.trim().isEmpty()) {
            this.textoAccion = texto;
            this.urlAccion = url.trim();
        }
        return this;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSaludo() {
        return saludo;
    }

    public List<String> getParrafos() {
        return parrafos;
    }

    public String getEtiquetaDestacado() {
        return etiquetaDestacado;
    }

    public String getDestacado() {
        return destacado;
    }

    public String getAdvertencia() {
        return advertencia;
    }

    public String getTextoAccion() {
        return textoAccion;
    }

    public String getUrlAccion() {
        return urlAccion;
    }

    public boolean tieneDestacado() {
        return destacado != null && !destacado.trim().isEmpty();
    }

    public boolean tieneAdvertencia() {
        return advertencia != null && !advertencia.trim().isEmpty();
    }

    public boolean tieneAccion() {
        return urlAccion != null && !urlAccion.trim().isEmpty();
    }
}
