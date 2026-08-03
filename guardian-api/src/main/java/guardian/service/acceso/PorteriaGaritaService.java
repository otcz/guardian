package guardian.service.acceso;

import guardian.dto.acceso.PorteriasGaritaResponse;
import guardian.security.UsuarioAutenticado;

/**
 * Las porterias como las ve la tablet de la garita.
 *
 * <p>Aparte del servicio de administracion porque responde otra pregunta y a
 * otro rol: aquel administra las puertas del conjunto, este solo dice entre
 * cuales puede elegir el guardia que acaba de entrar.</p>
 */
public interface PorteriaGaritaService {

    PorteriasGaritaResponse disponibles(UsuarioAutenticado guardia);
}
