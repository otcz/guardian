package guardian.service.residente;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.admin.PersonaRequest;
import guardian.dto.admin.VehiculoRequest;
import guardian.dto.admin.VehiculoResponse;
import guardian.dto.residente.FamiliarRequest;
import guardian.dto.residente.FamiliarResponse;
import guardian.dto.residente.VehiculoResidenteRequest;
import guardian.entity.base.BaseEntity;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.entity.vehiculo.GdVehiculo;
import guardian.exception.GuardianException;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.repository.GdVehiculoRepository;
import guardian.security.UsuarioAutenticado;
import guardian.service.admin.PersonaRegistrada;
import guardian.service.admin.PersonaService;
import guardian.service.admin.VehiculoService;
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
    private final GdVehiculoRepository vehiculoRepository;
    private final GdPersonaRepository personaRepository;
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
        alta.setDocumento(request.getDocumento());
        alta.setNombres(request.getNombres());
        alta.setApellidos(request.getApellidos());
        alta.setFechaNacimiento(request.getFechaNacimiento());
        alta.setFotoUrl(request.getFotoUrl());
        alta.setTelefono(request.getTelefono());
        alta.setCasaId(casa.getId());
        alta.setParentesco(request.getParentesco());

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

        if (activo) {
            exigirInhabilitacionDeLaCasa(vinculo.getPersona(), casa);
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
    public VehiculoResponse agregarVehiculo(VehiculoResidenteRequest request,
                                            UsuarioAutenticado usuario) {
        GdCasa casa = miCasa(usuario);

        VehiculoRequest alta = new VehiculoRequest();
        alta.setCasaId(casa.getId());
        alta.setPlaca(request.getPlaca());
        alta.setTipo(request.getTipo());
        alta.setMarca(request.getMarca());
        alta.setColor(request.getColor());

        return vehiculoService.crear(alta, usuario);
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

        if (activo) {
            exigirInhabilitacionDeLaCasa(vehiculo, casa);
        }

        return vehiculoService.cambiarEstado(vehiculoId, activo, usuario);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GdCasa miCasa(UsuarioAutenticado usuario) {
        return residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(usuario.getPersonaId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa)
                .orElseThrow(() -> GuardianException.solicitudInvalida(MensajesGlobales.SIN_CASA));
    }

    private void exigirTitular(UsuarioAutenticado usuario, GdCasa casa) {
        boolean esTitular = residenteCasaRepository
                .findByPersonaIdAndCasaId(usuario.getPersonaId(), casa.getId())
                .map(v -> Codigos.PARENTESCO_TITULAR.equals(v.getParentesco()))
                .orElse(false);
        if (!esTitular) {
            throw GuardianException.sinPermiso(MensajesGlobales.SOLO_TITULAR_FAMILIA);
        }
    }

    /**
     * Reactivar solo procede si la inhabilitacion la hizo alguien de la misma
     * casa. Si la hizo la administracion — o no se sabe quien — el residente
     * no puede revertirla: seria darle la vuelta a una sancion del conjunto
     * con dos toques en el celular.
     */
    private void exigirInhabilitacionDeLaCasa(BaseEntity entidad, GdCasa casa) {
        String quienInhabilito = entidad.getUsuarioModificador();

        boolean fueDeLaCasa = quienInhabilito != null && personaRepository
                .findByConjuntoIdAndDocumento(casa.getConjunto().getId(), quienInhabilito)
                .flatMap(p -> residenteCasaRepository
                        .findByPersonaIdAndCasaId(p.getId(), casa.getId()))
                .isPresent();

        if (!fueDeLaCasa) {
            throw GuardianException.sinPermiso(MensajesGlobales.REACTIVAR_SOLO_ADMIN);
        }
    }

    private FamiliarResponse mapear(GdResidenteCasa vinculo, UsuarioAutenticado usuario) {
        GdPersona persona = vinculo.getPersona();

        boolean tieneCredencial = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .isPresent();

        return FamiliarResponse.builder()
                .personaId(persona.getId())
                .documento(persona.getDocumento())
                .nombreCompleto(persona.getNombreCompleto())
                .parentesco(vinculo.getParentesco())
                .fotoUrl(persona.getFotoUrl())
                .edad(EdadUtil.calcular(persona.getFechaNacimiento()))
                .activo(persona.getActivo())
                .tieneCredencial(tieneCredencial)
                .esUsuarioActual(persona.getId().equals(usuario.getPersonaId()))
                .build();
    }
}
