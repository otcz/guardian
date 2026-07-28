package guardian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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

    public static void main(String[] args) {
        SpringApplication.run(GuardianApplication.class, args);
    }
}
