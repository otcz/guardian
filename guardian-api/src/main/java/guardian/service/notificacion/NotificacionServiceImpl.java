package guardian.service.notificacion;

import guardian.constant.Codigos;
import guardian.entity.acceso.GdInvitacion;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.persona.GdSolicitudHogar;
import guardian.entity.vehiculo.GdSolicitudVehiculo;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.repository.GdResidenteCasaRepository;
import guardian.util.TrasConfirmar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private final CorreoService correoService;
    private final GdResidenteCasaRepository residenteCasaRepository;

    /**
     * A donde mandar a quien lea el aviso. Vacia por defecto y entonces la
     * linea no se escribe: un correo que invita a "entrar a la aplicacion" sin
     * decir a donde es peor que uno que no lo menciona.
     */
    private final String urlApp;

    public NotificacionServiceImpl(CorreoService correoService,
                                   GdResidenteCasaRepository residenteCasaRepository,
                                   @Value("${guardian.app.url:}") String urlApp) {
        this.correoService = correoService;
        this.residenteCasaRepository = residenteCasaRepository;
        this.urlApp = urlApp;
    }

    // ── Solicitudes ──────────────────────────────────────────────────────────

    @Override
    public void solicitudVehiculoResuelta(GdSolicitudVehiculo solicitud, boolean aprobada) {
        GdPersona quien = solicitud.getSolicitante();
        String casa = solicitud.getCasa().getIdentificador();

        if (aprobada) {
            enviar(quien.getEmail(),
                    "Tu vehiculo " + solicitud.getPlaca() + " quedo autorizado",
                    saludo(quien.getNombres())
                            + "La administracion autorizo el vehiculo "
                            + solicitud.getPlaca() + " para la casa " + casa + ".\n\n"
                            + "Ya puede entrar y salir del conjunto."
                            + enlace());
            return;
        }

        enviar(quien.getEmail(),
                "No autorizaron el vehiculo " + solicitud.getPlaca(),
                saludo(quien.getNombres())
                        + "La administracion no autorizo el vehiculo "
                        + solicitud.getPlaca() + " para la casa " + casa + ".\n\n"
                        + motivo(solicitud.getMotivoRechazo())
                        + "Puedes corregir los datos y volver a pedirlo."
                        + enlace());
    }

    @Override
    public void solicitudHogarResuelta(GdSolicitudHogar solicitud, boolean aprobada) {
        String casa = solicitud.getCodigo().getCasa().getIdentificador();

        if (aprobada) {
            enviar(solicitud.getEmail(),
                    "Ya haces parte de la casa " + casa,
                    saludo(solicitud.getNombres())
                            + "La administracion aprobo tu solicitud para entrar a la casa "
                            + casa + ".\n\n"
                            + "Entra con tu numero de documento. Tu PIN es "
                            + Codigos.CLAVE_INICIAL
                            + " y tendras que cambiarlo la primera vez."
                            + enlace());
            return;
        }

        enviar(solicitud.getEmail(),
                "No aprobaron tu solicitud para la casa " + casa,
                saludo(solicitud.getNombres())
                        + "La administracion no aprobo tu solicitud para entrar a la casa "
                        + casa + ".\n\n"
                        + motivo(solicitud.getMotivoRechazo())
                        + "Escribe a la administracion del conjunto si crees que hay un error.");
    }

    @Override
    public void invitacionResuelta(GdInvitacion invitacion, boolean aprobada) {
        GdPersona anfitrion = invitacion.getAnfitrion();

        if (aprobada) {
            enviar(anfitrion.getEmail(),
                    "Tu invitado " + invitacion.getNombreInvitado() + " quedo autorizado",
                    saludo(anfitrion.getNombres())
                            + "La administracion autorizo la visita de "
                            + invitacion.getNombreInvitado() + ".\n\n"
                            + "Ya puedes compartirle su codigo de entrada."
                            + enlace());
            return;
        }

        enviar(anfitrion.getEmail(),
                "No autorizaron la visita de " + invitacion.getNombreInvitado(),
                saludo(anfitrion.getNombres())
                        + "La administracion no autorizo la visita de "
                        + invitacion.getNombreInvitado() + ".\n\n"
                        + motivo(invitacion.getMotivoRechazo())
                        + "Tu invitado no podra entrar con ese codigo."
                        + enlace());
    }

    // ── Bloqueos ─────────────────────────────────────────────────────────────

    @Override
    public void bloqueoPersonaCambiado(GdPersona persona, boolean bloqueada, String motivo) {
        // A ella Y al titular: la persona es quien se va a encontrar la
        // talanquera abajo, y el titular es quien responde por el hogar y a
        // quien va a ir a reclamarle. Si son la misma, se manda una sola vez.
        Set<String> destinos = new LinkedHashSet<>();
        agregar(destinos, persona.getEmail());
        agregar(destinos, correoDelTitular(casaDe(persona)));

        String nombre = persona.getNombreCompleto();

        for (String destino : destinos) {
            if (bloqueada) {
                enviar(destino,
                        nombre + " quedo deshabilitado en el conjunto",
                        "Hola,\n\n"
                                + nombre + " no puede entrar ni salir del conjunto hasta que "
                                + "la administracion vuelva a habilitarlo.\n\n"
                                + motivo(motivo)
                                + "Esto no se puede levantar desde la aplicacion. "
                                + "Escribe a la administracion del conjunto.");
            } else {
                enviar(destino,
                        nombre + " vuelve a tener acceso",
                        "Hola,\n\n"
                                + "La administracion habilito de nuevo a " + nombre + ".\n\n"
                                + "Ya puede entrar y salir del conjunto."
                                + enlace());
            }
        }
    }

    @Override
    public void bloqueoVehiculoCambiado(GdVehiculo vehiculo, boolean bloqueado, String motivo) {
        String destino = correoDelTitular(vehiculo.getCasa());
        String casa = vehiculo.getCasa().getIdentificador();

        if (bloqueado) {
            enviar(destino,
                    "El vehiculo " + vehiculo.getPlaca() + " quedo deshabilitado",
                    "Hola,\n\n"
                            + "El vehiculo " + vehiculo.getPlaca() + " de la casa " + casa
                            + " no puede entrar ni salir del conjunto hasta que la "
                            + "administracion vuelva a habilitarlo.\n\n"
                            + motivo(motivo)
                            + "Esto no se puede levantar desde la aplicacion. "
                            + "Escribe a la administracion del conjunto.");
            return;
        }

        enviar(destino,
                "El vehiculo " + vehiculo.getPlaca() + " vuelve a entrar",
                "Hola,\n\n"
                        + "La administracion habilito de nuevo el vehiculo "
                        + vehiculo.getPlaca() + " de la casa " + casa + ".\n\n"
                        + "Ya puede entrar y salir del conjunto."
                        + enlace());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String correoDelTitular(GdCasa casa) {
        if (casa == null) {
            return null;
        }
        return residenteCasaRepository
                .findFirstByCasaIdAndParentescoAndActivo(
                        casa.getId(), Codigos.PARENTESCO_TITULAR, Codigos.SI)
                .map(GdResidenteCasa::getPersona)
                .map(GdPersona::getEmail)
                .orElse(null);
    }

    /** La casa donde vive la persona, o null si todavia no vive en ninguna. */
    private GdCasa casaDe(GdPersona persona) {
        return residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa)
                .orElse(null);
    }

    /**
     * El envio real: siempre DESPUES de confirmar, y sin dejar que un fallo
     * suba. La aprobacion ya ocurrio; el aviso es un extra que no puede
     * tumbarla.
     */
    private void enviar(String destinatario, String asunto, String cuerpo) {
        if (destinatario == null || destinatario.trim().isEmpty()) {
            // No es un error: mucha gente del conjunto no tiene correo, y esa
            // persona simplemente se entera al abrir la aplicacion.
            return;
        }
        TrasConfirmar.ejecutar(() -> {
            try {
                correoService.enviar(destinatario, asunto, cuerpo);
            } catch (RuntimeException fallo) {
                // Cinturon y tirantes: CorreoService ya se compromete a no
                // lanzar, pero esto corre DESPUES del commit, donde una
                // excepcion no revierte nada y solo ensucia el log del
                // servidor con una traza sin dueno.
                log.error("[notificacion] no se pudo avisar '{}': {}",
                        asunto, fallo.getMessage());
            }
        });
    }

    private void agregar(Set<String> destinos, String correo) {
        if (correo != null && !correo.trim().isEmpty()) {
            destinos.add(correo.trim().toLowerCase());
        }
    }

    private String saludo(String nombres) {
        return "Hola " + nombres + ",\n\n";
    }

    /** Un rechazo sin motivo deja a la persona sin nada que corregir. */
    private String motivo(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "No dejaron un motivo registrado.\n\n";
        }
        return "Motivo: " + texto.trim() + "\n\n";
    }

    private String enlace() {
        if (urlApp == null || urlApp.trim().isEmpty()) {
            return "";
        }
        return "\n\nMiralo en " + urlApp.trim();
    }
}
