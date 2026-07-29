package guardian.dto.admin;

import guardian.dto.acceso.AccesoEventoResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Estado del conjunto de un vistazo: la pantalla de aterrizaje del
 * administrador. Un solo endpoint para que el tablero no dispare seis
 * consultas al abrir.
 */
@Data
@Builder
public class ResumenResponse {

    // ── Presencia en vivo ────────────────────────────────────────────────────
    private long adentro;
    private long afuera;

    // ── Inventario ───────────────────────────────────────────────────────────
    private long casasActivas;
    private long casasTotal;
    private long personasActivas;
    private long personasTotal;
    private long vehiculosActivos;
    private long vehiculosTotal;
    private long usuariosActivos;
    private long usuariosTotal;

    // ── Actividad de hoy ─────────────────────────────────────────────────────
    private long eventosHoy;
    private long permitidosHoy;
    private long denegadosHoy;

    /** Los movimientos mas recientes, para no abrir la bitacora por todo. */
    private List<AccesoEventoResponse> ultimosMovimientos;
}
