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
        String placa = solicitud.getPlaca();

        if (aprobada) {
            enviar(quien.getEmail(), MensajeCorreo
                    .de("Tu vehículo " + placa + " quedó autorizado",
                            "Vehículo autorizado",
                            "Hola " + quien.getNombres() + ",")
                    .parrafo("La administración autorizó tu vehículo para la casa "
                            + casa + ". Ya puede entrar y salir del conjunto.")
                    .destacado("Placa autorizada", placa)
                    .parrafo("La portería lo reconocerá desde ahora. No necesitas "
                            + "hacer nada más.")
                    .accion("Ver mis vehículos", urlApp));
            return;
        }

        enviar(quien.getEmail(), MensajeCorreo
                .de("No autorizaron el vehículo " + placa,
                        "Vehículo no autorizado",
                        "Hola " + quien.getNombres() + ",")
                .parrafo("La administración revisó tu solicitud para la casa "
                        + casa + " y no la autorizó.")
                .destacado("Placa", placa)
                .parrafo(motivo(solicitud.getMotivoRechazo()))
                .advertencia("Puedes corregir los datos y volver a pedirlo desde "
                        + "la aplicación. Este vehículo no entra al conjunto "
                        + "mientras tanto.")
                .accion("Volver a pedirlo", urlApp));
    }

    @Override
    public void solicitudHogarResuelta(GdSolicitudHogar solicitud, boolean aprobada) {
        String casa = solicitud.getCodigo().getCasa().getIdentificador();

        if (aprobada) {
            enviar(solicitud.getEmail(), MensajeCorreo
                    .de("Ya haces parte de la casa " + casa,
                            "Bienvenido a " + casa,
                            "Hola " + solicitud.getNombres() + ",")
                    .parrafo("La administración aprobó tu solicitud. Ya estás "
                            + "registrado en la casa " + casa + " y puedes entrar "
                            + "a la aplicación.")
                    .destacado("Tu usuario", solicitud.getDocumento())
                    .parrafo("Tu PIN de entrada es " + Codigos.CLAVE_INICIAL
                            + ". La aplicación te va a pedir que lo cambies la "
                            + "primera vez.")
                    .advertencia("Cámbialo apenas entres y no lo compartas: es lo "
                            + "que abre tu código de acceso en la portería.")
                    .accion("Entrar a GUARDIAN", urlApp));
            return;
        }

        enviar(solicitud.getEmail(), MensajeCorreo
                .de("No aprobaron tu solicitud para la casa " + casa,
                        "Solicitud no aprobada",
                        "Hola " + solicitud.getNombres() + ",")
                .parrafo("La administración revisó tu solicitud para entrar a la "
                        + "casa " + casa + " y no la aprobó.")
                .parrafo(motivo(solicitud.getMotivoRechazo()))
                .advertencia("Si crees que hay un error, escribe a la administración "
                        + "del conjunto."));
    }

    @Override
    public void invitacionResuelta(GdInvitacion invitacion, boolean aprobada) {
        GdPersona anfitrion = invitacion.getAnfitrion();
        String invitado = invitacion.getNombreInvitado();

        if (aprobada) {
            enviar(anfitrion.getEmail(), MensajeCorreo
                    .de("Tu invitado " + invitado + " quedó autorizado",
                            "Visita autorizada",
                            "Hola " + anfitrion.getNombres() + ",")
                    .parrafo("La administración autorizó la visita que registraste. "
                            + "Ya puedes compartirle su código de entrada.")
                    .destacado("Invitado", invitado)
                    .parrafo("El código solo sirve dentro de las fechas que "
                            + "indicaste al invitarlo.")
                    .accion("Ver la invitación", urlApp));
            return;
        }

        enviar(anfitrion.getEmail(), MensajeCorreo
                .de("No autorizaron la visita de " + invitado,
                        "Visita no autorizada",
                        "Hola " + anfitrion.getNombres() + ",")
                .parrafo("La administración revisó la visita que registraste y no "
                        + "la autorizó.")
                .destacado("Invitado", invitado)
                .parrafo(motivo(invitacion.getMotivoRechazo()))
                .advertencia("Tu invitado no podrá entrar con ese código. "
                        + "Avísale antes de que venga."));
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
                enviar(destino, MensajeCorreo
                        .de(nombre + " quedó deshabilitado en el conjunto",
                                "Acceso deshabilitado",
                                "Hola,")
                        .parrafo("La administración deshabilitó el acceso de "
                                + nombre + ". La portería no le va a permitir "
                                + "entrar ni salir.")
                        .parrafo(motivo(motivo))
                        .advertencia("Esto no se puede levantar desde la aplicación. "
                                + "Para resolverlo, escribe a la administración del "
                                + "conjunto."));
            } else {
                enviar(destino, MensajeCorreo
                        .de(nombre + " vuelve a tener acceso",
                                "Acceso restablecido",
                                "Hola,")
                        .parrafo("La administración habilitó de nuevo a " + nombre
                                + ". Ya puede entrar y salir del conjunto con "
                                + "normalidad.")
                        .accion("Abrir GUARDIAN", urlApp));
            }
        }
    }

    @Override
    public void bloqueoVehiculoCambiado(GdVehiculo vehiculo, boolean bloqueado, String motivo) {
        String destino = correoDelTitular(vehiculo.getCasa());
        String casa = vehiculo.getCasa().getIdentificador();
        String placa = vehiculo.getPlaca();

        if (bloqueado) {
            enviar(destino, MensajeCorreo
                    .de("El vehículo " + placa + " quedó deshabilitado",
                            "Vehículo deshabilitado",
                            "Hola,")
                    .parrafo("La administración deshabilitó un vehículo de la casa "
                            + casa + ". La portería no le va a permitir entrar ni "
                            + "salir.")
                    .destacado("Placa", placa)
                    .parrafo(motivo(motivo))
                    .advertencia("Esto no se puede levantar desde la aplicación. "
                            + "Para resolverlo, escribe a la administración del "
                            + "conjunto."));
            return;
        }

        enviar(destino, MensajeCorreo
                .de("El vehículo " + placa + " vuelve a entrar",
                        "Vehículo habilitado",
                        "Hola,")
                .parrafo("La administración habilitó de nuevo un vehículo de la "
                        + "casa " + casa + ". Ya puede entrar y salir con "
                        + "normalidad.")
                .destacado("Placa", placa)
                .accion("Ver mis vehículos", urlApp));
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
    private void enviar(String destinatario, MensajeCorreo mensaje) {
        if (destinatario == null || destinatario.trim().isEmpty()) {
            // No es un error: mucha gente del conjunto no tiene correo, y esa
            // persona simplemente se entera al abrir la aplicación.
            return;
        }
        TrasConfirmar.ejecutar(() -> {
            try {
                correoService.enviar(destinatario, mensaje);
            } catch (RuntimeException fallo) {
                // Cinturón y tirantes: CorreoService ya se compromete a no
                // lanzar, pero esto corre DESPUÉS del commit, donde una
                // excepción no revierte nada y solo ensucia el log del
                // servidor con una traza sin dueño.
                log.error("[notificación] no se pudo avisar '{}': {}",
                        mensaje.getAsunto(), fallo.getMessage());
            }
        });
    }

    private void agregar(Set<String> destinos, String correo) {
        if (correo != null && !correo.trim().isEmpty()) {
            destinos.add(correo.trim().toLowerCase());
        }
    }

    /** Un rechazo sin motivo deja a la persona sin nada que corregir. */
    private String motivo(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "La administración no dejó un motivo registrado.";
        }
        return "Motivo: " + texto.trim();
    }
}
