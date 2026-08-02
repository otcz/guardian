package guardian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Punto de entrada de GUARDIAN.
 *
 * <p>Monolito por decision de arquitectura, no por falta de tiempo: el dominio
 * es pequeno, todo comparte la misma base y el cold start de un microservicio
 * Java en la garita costaria 5-15 segundos justo cuando hay gente esperando.
 * Ver .claude/CLAUDE.md seccion 8.</p>
 */
@SpringBootApplication
public class GuardianApplication {

    /**
     * Zona en la que la aplicacion interpreta TODA fecha.
     *
     * <p>Las 34 columnas de fecha son {@code timestamp without time zone}: lo
     * que queda guardado es la hora de PARED, no el instante. Eso funciona
     * mientras la zona con la que se escribe sea la misma con la que se lee —
     * y hoy funciona porque la maquina de desarrollo esta en Colombia.</p>
     *
     * <p>Se rompe al desplegar. Cloud Run corre en UTC, asi que la fila que
     * dice {@code 10:36} se leeria como 10:36 UTC en vez de 10:36 de Colombia:
     * cinco horas de corrimiento en la bitacora, en los vencimientos de las
     * invitaciones y en los QR. Y las filas nuevas quedarian en UTC, mezcladas
     * con las viejas en local, sin forma de distinguirlas despues.</p>
     *
     * <p>Fijarla aca lo cierra sin migrar un solo dato: la interpretacion deja
     * de depender de donde corra el proceso. Las alternativas —guardar en UTC
     * o migrar a {@code timestamptz}— exigen reescribir lo ya guardado, y un
     * conjunto residencial no tiene operacion en dos zonas que lo justifique.</p>
     */
    private static final String ZONA_POR_DEFECTO = "America/Bogota";

    public static void main(String[] args) {
        fijarZonaHoraria();
        SpringApplication.run(GuardianApplication.class, args);
    }

    /**
     * ANTES de {@code SpringApplication.run}, no en un {@code @PostConstruct}.
     *
     * <p>El pool de conexiones y Hibernate leen la zona por defecto de la JVM
     * al inicializarse. Cambiarla despues dejaria una parte del sistema con la
     * zona vieja — el peor de los mundos, porque el corrimiento aparecería solo
     * en algunas consultas.</p>
     *
     * <p>Visible para el paquete y no privado para que el test pueda
     * comprobarlo sin levantar Spring entero.</p>
     */
    static void fijarZonaHoraria() {
        String pedida = System.getenv("GUARDIAN_ZONA_HORARIA");
        String zona = (pedida == null || pedida.trim().isEmpty())
                ? ZONA_POR_DEFECTO
                : pedida.trim();

        TimeZone elegida = TimeZone.getTimeZone(zona);

        // getTimeZone devuelve GMT EN SILENCIO cuando el nombre no existe. Un
        // "America/Bogata" mal escrito en una variable de entorno dejaria el
        // sistema en UTC con todo pareciendo normal, que es exactamente el
        // corrimiento de cinco horas que este metodo existe para evitar.
        if (!elegida.getID().equals(zona)) {
            System.err.println("[guardian] zona horaria desconocida: '" + zona
                    + "'. Uso " + ZONA_POR_DEFECTO + ".");
            elegida = TimeZone.getTimeZone(ZONA_POR_DEFECTO);
        }

        TimeZone.setDefault(elegida);
        // System.out y no un logger: esto corre antes de que exista el contexto
        // de Spring, asi que todavia no hay logging configurado.
        System.out.println("[guardian] zona horaria: " + TimeZone.getDefault().getID());
    }
}
