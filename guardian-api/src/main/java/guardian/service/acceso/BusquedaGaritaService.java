package guardian.service.acceso;

import guardian.dto.acceso.CandidatoGaritaResponse;
import guardian.security.UsuarioAutenticado;

import java.util.List;

/**
 * Buscar a alguien por su nombre desde la portería.
 *
 * <p>Es el respaldo del escaneo, no un camino alterno. El guardia identifica
 * con el lector; esto existe para cuando el QR no carga, el teléfono se quedó
 * sin batería o la persona llegó sin cédula. Encontrarla NO la autoriza: elegir
 * un candidato dispara la verificación normal por documento, que es la que
 * decide.</p>
 *
 * <p>Vive aparte de {@code AccesoService} porque aquel ya pasa de seiscientas
 * líneas resolviendo el paso, y esto no comparte nada con esa lógica.</p>
 */
public interface BusquedaGaritaService {

    /**
     * Candidatos dentro de la sede del guardia.
     *
     * <p>Devuelve lista vacía —nunca error— cuando el texto es muy corto o no
     * coincide con nadie: esto se llama mientras alguien teclea, y un 400 a
     * mitad de una palabra convertiría cada letra en un error en pantalla.</p>
     */
    List<CandidatoGaritaResponse> buscar(String texto, UsuarioAutenticado guardia);
}
