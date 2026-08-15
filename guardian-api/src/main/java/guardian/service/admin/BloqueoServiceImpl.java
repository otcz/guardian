package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.entity.base.BaseEntity;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdCasaRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.security.EstadoUsuarioService;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloqueoServiceImpl implements BloqueoService {

    private final GdPersonaRepository personaRepository;
    private final GdVehiculoRepository vehiculoRepository;
    private final GdCasaRepository casaRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final EstadoUsuarioService estadoUsuarioService;
    private final guardian.service.notificacion.NotificacionService notificacionService;

    // ── Personas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void bloquearPersona(Long id, String motivo, UsuarioAutenticado admin) {
        // Sin chequeo aparte: personaDe() ya deja fuera la del ejecutor, que
        // es lo que impide que un administrador se bloquee y quede sin nadie
        // adentro capaz de revertirlo.
        GdPersona persona = personaDe(id, admin);

        aplicarBloqueo(persona, motivo, admin, "persona", id);
        personaRepository.save(persona);
        invalidarSesionDe(id);

        // El aviso mas importante de los cuatro: a esta persona la porteria le
        // va a negar el paso y, sin correo, se entera parada frente al guardia.
        notificacionService.bloqueoPersonaCambiado(persona, true, motivo);
    }

    @Override
    @Transactional
    public void desbloquearPersona(Long id, UsuarioAutenticado admin) {
        GdPersona persona = personaDe(id, admin);
        quitarBloqueo(persona, admin, "persona", id);
        personaRepository.save(persona);
        invalidarSesionDe(id);

        notificacionService.bloqueoPersonaCambiado(persona, false, null);
    }

    // ── Vehiculos ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void bloquearVehiculo(Long id, String motivo, UsuarioAutenticado admin) {
        GdVehiculo vehiculo = vehiculoDe(id, admin);
        aplicarBloqueo(vehiculo, motivo, admin, "vehiculo", id);
        vehiculoRepository.save(vehiculo);

        notificacionService.bloqueoVehiculoCambiado(vehiculo, true, motivo);
    }

    @Override
    @Transactional
    public void desbloquearVehiculo(Long id, UsuarioAutenticado admin) {
        GdVehiculo vehiculo = vehiculoDe(id, admin);
        quitarBloqueo(vehiculo, admin, "vehiculo", id);
        vehiculoRepository.save(vehiculo);

        notificacionService.bloqueoVehiculoCambiado(vehiculo, false, null);
    }

    // ── Casas ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void bloquearCasa(Long id, String motivo, UsuarioAutenticado admin) {
        GdCasa casa = casaDe(id, admin);
        aplicarBloqueo(casa, motivo, admin, "casa", id);
        casaRepository.save(casa);
    }

    @Override
    @Transactional
    public void desbloquearCasa(Long id, UsuarioAutenticado admin) {
        GdCasa casa = casaDe(id, admin);
        quitarBloqueo(casa, admin, "casa", id);
        casaRepository.save(casa);
    }

    // ── Cuentas (guardias incluidos) ─────────────────────────────────────────

    @Override
    @Transactional
    public void bloquearUsuario(Long id, String motivo, UsuarioAutenticado admin) {
        GdUsuario usuario = usuarioDe(id, admin);

        aplicarBloqueo(usuario, motivo, admin, "usuario", id);
        usuarioRepository.save(usuario);
        // Sin esto, el guardia recien bloqueado sigue escaneando hasta que
        // expire su token.
        estadoUsuarioService.invalidar(id);
    }

    @Override
    @Transactional
    public void desbloquearUsuario(Long id, UsuarioAutenticado admin) {
        GdUsuario usuario = usuarioDe(id, admin);
        quitarBloqueo(usuario, admin, "usuario", id);
        usuarioRepository.save(usuario);
        estadoUsuarioService.invalidar(id);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void aplicarBloqueo(BaseEntity entidad, String motivo,
                                UsuarioAutenticado admin, String tipo, Long id) {
        entidad.setBloqueado(Codigos.SI);
        entidad.setMotivoBloqueo(motivo);
        entidad.setUsuarioModificador(admin.getDocumento());

        log.warn("[bloqueo] {} id={} BLOQUEADO por={} motivo={}",
                tipo, id, admin.getDocumento(), motivo);
    }

    private void quitarBloqueo(BaseEntity entidad, UsuarioAutenticado admin,
                               String tipo, Long id) {
        entidad.setBloqueado(Codigos.NO);
        entidad.setMotivoBloqueo(null);
        entidad.setUsuarioModificador(admin.getDocumento());

        log.warn("[bloqueo] {} id={} DESBLOQUEADO por={}", tipo, id, admin.getDocumento());
    }

    /** La cuenta de esa persona, si tiene, para que su sesion muera de una vez. */
    private void invalidarSesionDe(Long personaId) {
        usuarioRepository.findByPersonaId(personaId)
                .ifPresent(u -> estadoUsuarioService.invalidar(u.getId()));
    }

    // ── Resolucion con frontera de sede ──────────────────────────────────────

    /**
     * La persona del ejecutor no existe para el: mismo 404 que la de otra
     * sede. Antes se resolvia y se frenaba despues con un 400 que decia "no
     * puedes inactivarte a ti mismo" — un mensaje distinto confirma que el
     * registro existe, y la regla quedaba repetida accion por accion.
     */
    private GdPersona personaDe(Long id, UsuarioAutenticado admin) {
        return personaRepository.findById(id)
                .filter(p -> !p.getId().equals(admin.getPersonaId()))
                .filter(p -> p.getConjunto().getId().equals(admin.getConjuntoId()))
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }

    private GdVehiculo vehiculoDe(Long id, UsuarioAutenticado admin) {
        return vehiculoRepository.findById(id)
                .filter(v -> v.getConjuntoId().equals(admin.getConjuntoId()))
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }

    private GdCasa casaDe(Long id, UsuarioAutenticado admin) {
        return casaRepository.findById(id)
                .filter(c -> c.getConjunto().getId().equals(admin.getConjuntoId()))
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }

    private GdUsuario usuarioDe(Long id, UsuarioAutenticado admin) {
        return usuarioRepository.findById(id)
                .filter(u -> !u.getId().equals(admin.getUsuarioId()))
                .filter(u -> u.getPersona().getConjunto().getId().equals(admin.getConjuntoId()))
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }
}
