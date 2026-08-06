package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.dto.residente.FamiliarRequest;
import guardian.dto.residente.FamiliarResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.PersonaRegistrada;
import guardian.service.admin.PersonaService;
import guardian.service.admin.VehiculoService;
import guardian.util.CorreoUtil;
import guardian.util.EdadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Delegación consciente: el alta de personas y vehiculos ya vive en los
 * services de administracion; aca solo se fuerza que TODO ocurra sobre la casa
 * del solicitante y se vetan las operaciones que no le corresponden.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutogestionServiceImpl implements AutogestionService {

    private final GdResidenteCasaRepository residenteCasaRepository;
    private final GdCredencialQrRepository credencialRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final GdVehiculoRepository vehiculoRepository;
    private final HogarDelResidente hogar;
    private final PersonaService personaService;
    private final VehiculoService vehiculoService;

    // ── Familia ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<FamiliarResponse> listarFamilia(UsuarioAutenticado usuario) {
        GdCasa casa = miCasa(usuario);

        return residenteCasaRepository.findByCasaIdAndActivo(casa.getId(), Codigos.SI)
                .stream()
                .map(vinculo -> mapear(vinculo, usuario))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonaRegistrada agregarFamiliar(FamiliarRequest request, UsuarioAutenticado usuario) {
        GdCasa casa = miCasa(usuario);

        // CONTEXT.md seccion 2: quien administra la familia es el TITULAR. Un
        // hijo o un "OTRO" de la casa ve la lista, pero no agrega gente.
        exigirTitular(usuario, casa);

        // El titular lo asigna la administracion: si cualquier residente
        // pudiera nombrarse titular, la regla de titular unico por casa
        // perderia todo su sentido.
        if (Codigos.PARENTESCO_TITULAR.equals(request.getParentesco())) {
            throw GuardianException.sinPermiso(MensajesGlobales.TITULAR_SOLO_ADMIN);
        }

        PersonaRequest alta = new PersonaRequest();
        alta.setTipoDocumento(request.getTipoDocumento());
        alta.setDocumento(request.getDocumento());
        alta.setNombres(request.getNombres());
        alta.setApellidos(request.getApellidos());
        alta.setFechaNacimiento(request.getFechaNacimiento());
        alta.setFotoUrl(request.getFotoUrl());
        alta.setTelefono(request.getTelefono());
        alta.setEmail(request.getEmail());
        alta.setCasaId(casa.getId());
        alta.setParentesco(request.getParentesco());

        // Con correo, el familiar entra a la aplicacion: ve su QR, sus datos y
        // los vehiculos de la casa. Sin cuenta quedaba registrado para el
        // guardia pero no podia abrir la aplicacion, y el login le decia que su
        // PIN era 0000 — una promesa que nadie cumplia.
        //
        // El rol se fija ACA y no se lee del request: si viniera de afuera, un
        // titular podria crearse un ADMIN desde el celular.
        if (CorreoUtil.normalizar(request.getEmail()) != null) {
            alta.setRolUsuario(Codigos.ROL_RESIDENTE);
        }

        PersonaRegistrada registrada = personaService.crear(alta, usuario);
        log.info("[autogestion] familiar agregado personaId={} casaId={} por={}",
                registrada.getPersona().getId(), casa.getId(), usuario.getDocumento());
        return registrada;
    }

    @Override
    @Transactional
    public FamiliarResponse cambiarEstadoFamiliar(Long personaId, boolean activo,
                                                  UsuarioAutenticado usuario) {
        GdCasa casa = miCasa(usuario);
        exigirTitular(usuario, casa);

        if (personaId.equals(usuario.getPersonaId())) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.NO_INACTIVARSE_A_SI_MISMO);
        }

        GdResidenteCasa vinculo = residenteCasaRepository
                .findByPersonaIdAndCasaId(personaId, casa.getId())
                .orElseThrow(() -> GuardianException.sinPermiso(MensajesGlobales.FAMILIAR_AJENO));

        // Lo que la administracion bloqueo no lo levanta el celular de nadie.
        if (vinculo.getPersona().estaBloqueado()) {
            throw GuardianException.sinPermiso(MensajesGlobales.DESBLOQUEO_SOLO_ADMIN);
        }

        personaService.cambiarEstado(personaId, activo, usuario);
        return mapear(vinculo, usuario);
    }

    // ── Vehiculos ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarMisVehiculos(UsuarioAutenticado usuario) {
        return vehiculoService.listarPorCasaIncluyendoInactivos(
                miCasa(usuario).getId(), usuario.getConjuntoId());
    }

    @Override
    @Transactional
    public VehiculoResponse cambiarEstadoVehiculo(Long vehiculoId, boolean activo,
                                                  UsuarioAutenticado usuario) {
        GdCasa casa = miCasa(usuario);

        // Solo vehiculos de MI casa: sin esto, cualquier residente podria
        // inhabilitar el carro del vecino adivinando el id.
        GdVehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .filter(v -> v.getCasa().getId().equals(casa.getId()))
                .orElseThrow(() -> GuardianException.sinPermiso(MensajesGlobales.FAMILIAR_AJENO));

        if (vehiculo.estaBloqueado()) {
            throw GuardianException.sinPermiso(MensajesGlobales.DESBLOQUEO_SOLO_ADMIN);
        }

        return vehiculoService.cambiarEstado(vehiculoId, activo, usuario);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdCasa miCasa(UsuarioAutenticado usuario) {
        return hogar.casa(usuario);
    }

    private void exigirTitular(UsuarioAutenticado usuario, GdCasa casa) {
        hogar.exigirTitular(usuario, casa, MensajesGlobales.SOLO_TITULAR_FAMILIA);
    }

    // La heuristica de "quien lo inhabilito" desaparecio: ahora hay dos
    // llaves reales (activo del residente, bloqueado del administrador) y no
    // hace falta adivinar quien apago el interruptor mirando el auditor.

    private FamiliarResponse mapear(GdResidenteCasa vinculo, UsuarioAutenticado usuario) {
        GdPersona persona = vinculo.getPersona();

        boolean tieneCredencial = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .isPresent();

        return FamiliarResponse.builder()
                .personaId(persona.getId())
                .tipoDocumento(persona.getTipoDocumento())
                .documento(persona.getDocumento())
                .nombreCompleto(persona.getNombreCompleto())
                .parentesco(vinculo.getParentesco())
                .fotoUrl(persona.getFotoUrl())
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .activo(persona.getActivo())
                .bloqueado(persona.getBloqueado())
                .motivoBloqueo(persona.getMotivoBloqueo())
                .tieneCredencial(tieneCredencial)
                .tieneCuenta(usuarioRepository.existsByPersonaId(persona.getId()))
                .esUsuarioActual(persona.getId().equals(usuario.getPersonaId()))
                .build();
    }
}
