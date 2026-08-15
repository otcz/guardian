package guardian.service.notificacion;

/**
 * Viste un {@link MensajeCorreo} con la identidad de GUARDIAN.
 *
 * <p><b>Tablas y estilos en línea, no CSS moderno.</b> Los clientes de correo
 * van veinte años atrás del navegador: Outlook renderiza con el motor de Word,
 * Gmail recorta las hojas de estilo y casi ninguno entiende flexbox. Lo que
 * aquí parece anticuado es lo único que se ve igual en todos.</p>
 *
 * <p><b>Sin imágenes, ni siquiera el escudo.</b> Gmail bloquea las imágenes por
 * defecto en remitentes que no conoces, así que un encabezado con logo se ve
 * como un recuadro roto justo en el primer correo que alguien recibe — que es
 * el que decide si el sistema le parece serio. La marca se construye con color
 * y tipografía, que nadie puede bloquear.</p>
 *
 * <p>Cada correo va en multiparte: HTML para quien pueda verlo y texto plano
 * para el resto. El texto plano no es un descarte, es la versión que leen los
 * relojes, los lectores de pantalla y quien tiene el HTML desactivado.</p>
 */
final class PlantillaCorreo {

    /* Paleta de marca. Duplicada del frontend a proposito: un correo no puede
       leer tokens CSS, y meter aqui una dependencia del guardian-ui para seis
       colores seria peor que estas seis constantes. */
    private static final String NAVY = "#12233a";
    private static final String NAVY_TENUE = "#9fb0c4";
    private static final String ORO = "#d9a62e";
    private static final String ORO_OSCURO = "#96701c";
    private static final String ORO_TENUE = "#faf1dc";
    private static final String GRIS_FONDO = "#eef0f4";
    private static final String GRIS_BORDE = "#e3e6eb";
    private static final String GRIS_TEXTO = "#5a6472";
    private static final String GRIS_SUAVE = "#8a93a1";
    private static final String TEXTO_FUERTE = "#2b3440";

    /** Pila con respaldos: ninguna fuente de marca sobrevive a un correo. */
    private static final String FUENTE =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif";

    private PlantillaCorreo() {
    }

    static String html(MensajeCorreo mensaje, String nombreRemitente) {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("<!DOCTYPE html><html lang=\"es\"><head>")
                .append("<meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(escapar(mensaje.getAsunto())).append("</title>")
                .append("</head>")
                .append("<body style=\"margin:0;padding:0;background:").append(GRIS_FONDO)
                .append(";\">");

        // Preencabezado: la linea que Gmail muestra al lado del asunto en la
        // bandeja. Sin esto, ahi se cuela el primer texto del cuerpo, que suele
        // ser el saludo y no dice nada.
        sb.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;\">")
                .append(escapar(primerParrafo(mensaje)))
                .append("</div>");

        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"background:").append(GRIS_FONDO)
                .append(";padding:24px 12px;\"><tr><td align=\"center\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" ")
                .append("style=\"max-width:560px;background:#ffffff;border:1px solid ")
                .append(GRIS_BORDE).append(";border-radius:12px;overflow:hidden;\">");

        // ── Cabecera de marca ────────────────────────────────────────────────
        sb.append("<tr><td style=\"background:").append(NAVY)
                .append(";padding:22px 28px;border-bottom:3px solid ").append(ORO).append(";\">")
                .append("<div style=\"font-family:").append(FUENTE)
                .append(";font-size:20px;font-weight:700;letter-spacing:.22em;color:")
                .append(ORO).append(";\">GUARDIAN</div>")
                .append("<div style=\"font-family:").append(FUENTE)
                .append(";font-size:12px;color:").append(NAVY_TENUE)
                .append(";padding-top:6px;\">Control de acceso del conjunto</div>")
                .append("</td></tr>");

        // ── Cuerpo ───────────────────────────────────────────────────────────
        sb.append("<tr><td style=\"padding:28px 28px 8px;\">")
                .append("<h1 style=\"margin:0 0 18px;font-family:").append(FUENTE)
                .append(";font-size:19px;line-height:1.35;font-weight:700;color:")
                .append(NAVY).append(";\">").append(escapar(mensaje.getTitulo())).append("</h1>");

        if (mensaje.getSaludo() != null) {
            sb.append(parrafo(escapar(mensaje.getSaludo())));
        }
        for (String p : mensaje.getParrafos()) {
            sb.append(parrafo(escapar(p)));
        }

        if (mensaje.tieneDestacado()) {
            sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                    .append("style=\"margin:20px 0;\"><tr><td align=\"center\" ")
                    .append("style=\"background:").append(ORO_TENUE)
                    .append(";border:1px solid ").append(ORO)
                    .append(";border-radius:10px;padding:18px 16px;\">");
            if (mensaje.getEtiquetaDestacado() != null) {
                sb.append("<div style=\"font-family:").append(FUENTE)
                        .append(";font-size:11px;font-weight:700;letter-spacing:.12em;")
                        .append("text-transform:uppercase;color:").append(ORO_OSCURO)
                        .append(";padding-bottom:8px;\">")
                        .append(escapar(mensaje.getEtiquetaDestacado())).append("</div>");
            }
            sb.append("<div style=\"font-family:").append(FUENTE)
                    .append(";font-size:26px;font-weight:700;letter-spacing:.14em;color:")
                    .append(NAVY).append(";\">")
                    .append(escapar(mensaje.getDestacado())).append("</div>")
                    .append("</td></tr></table>");
        }

        if (mensaje.tieneAdvertencia()) {
            // Barra lateral y no fondo de color: un bloque entero coloreado se
            // lee como publicidad, y esto es justo lo que no se puede ignorar.
            sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                    .append("style=\"margin:18px 0;\"><tr><td ")
                    .append("style=\"border-left:3px solid ").append(ORO)
                    .append(";padding:2px 0 2px 14px;font-family:").append(FUENTE)
                    .append(";font-size:14px;line-height:1.55;color:").append(GRIS_TEXTO)
                    .append(";\">").append(escapar(mensaje.getAdvertencia()))
                    .append("</td></tr></table>");
        }

        if (mensaje.tieneAccion()) {
            sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" ")
                    .append("style=\"margin:22px 0 8px;\"><tr><td ")
                    .append("style=\"background:").append(NAVY).append(";border-radius:8px;\">")
                    .append("<a href=\"").append(escapar(mensaje.getUrlAccion()))
                    .append("\" style=\"display:inline-block;padding:12px 26px;font-family:")
                    .append(FUENTE).append(";font-size:15px;font-weight:600;color:#ffffff;")
                    .append("text-decoration:none;\">")
                    .append(escapar(mensaje.getTextoAccion())).append("</a>")
                    .append("</td></tr></table>");
        }

        sb.append("</td></tr>");

        // ── Pie ──────────────────────────────────────────────────────────────
        sb.append("<tr><td style=\"padding:18px 28px 24px;border-top:1px solid ")
                .append(GRIS_BORDE).append(";\">")
                .append("<div style=\"font-family:").append(FUENTE)
                .append(";font-size:12px;line-height:1.6;color:").append(GRIS_SUAVE)
                .append(";\">")
                .append(escapar(nombreRemitente))
                .append(" &middot; mensaje automático<br>")
                .append("No respondas a este correo. Para cualquier duda, escribe a la "
                        + "administración del conjunto.")
                .append("</div></td></tr>");

        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
    }

    /**
     * La misma información en texto plano.
     *
     * <p>No es un descarte del HTML: es lo que leen los relojes, los lectores
     * de pantalla y quien desactivó el HTML. Un multiparte cuya rama de texto
     * dice "abre esto en un cliente moderno" es un correo a medio escribir.</p>
     */
    static String texto(MensajeCorreo mensaje, String nombreRemitente) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(mensaje.getTitulo()).append("\n");
        for (int i = 0; i < mensaje.getTitulo().length(); i++) {
            sb.append('=');
        }
        sb.append("\n\n");

        if (mensaje.getSaludo() != null) {
            sb.append(mensaje.getSaludo()).append("\n\n");
        }
        for (String p : mensaje.getParrafos()) {
            sb.append(p).append("\n\n");
        }
        if (mensaje.tieneDestacado()) {
            if (mensaje.getEtiquetaDestacado() != null) {
                sb.append(mensaje.getEtiquetaDestacado()).append(":\n");
            }
            sb.append("    ").append(mensaje.getDestacado()).append("\n\n");
        }
        if (mensaje.tieneAdvertencia()) {
            sb.append(mensaje.getAdvertencia()).append("\n\n");
        }
        if (mensaje.tieneAccion()) {
            sb.append(mensaje.getTextoAccion()).append(": ")
                    .append(mensaje.getUrlAccion()).append("\n\n");
        }

        sb.append("-- \n").append(nombreRemitente).append(" · mensaje automático\n")
                .append("No respondas a este correo. Para cualquier duda, escribe a la\n")
                .append("administración del conjunto.");
        return sb.toString();
    }

    private static String parrafo(String texto) {
        return "<p style=\"margin:0 0 14px;font-family:" + FUENTE
                + ";font-size:15px;line-height:1.6;color:" + TEXTO_FUERTE + ";\">"
                + texto + "</p>";
    }

    private static String primerParrafo(MensajeCorreo mensaje) {
        return mensaje.getParrafos().isEmpty() ? mensaje.getTitulo()
                : mensaje.getParrafos().get(0);
    }

    /**
     * Los datos que entran al correo vienen de formularios —el nombre de un
     * invitado, el motivo que escribió la administración—, así que un "&lt;"
     * suelto rompería la maqueta y un fragmento de HTML pegado ahí se
     * ejecutaría en el cliente de quien lo recibe.
     */
    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
