package guardian.service.admin;

import guardian.dto.admin.ConteoSolicitudesResponse;

/**
 * Cuantas decisiones tiene esperando el administrador, sumando las dos
 * bandejas.
 *
 * <p>Existe para que el aviso del menu sea UNA peticion. Cada bandeja cuenta lo
 * suyo, pero el numerito que el administrador ve es uno solo, y pedirlo con dos
 * llamadas en cada navegacion del panel seria pagar dos veces por un numero.</p>
 */
public interface SolicitudesResumenService {

    ConteoSolicitudesResponse conteo(Long conjuntoId);
}
