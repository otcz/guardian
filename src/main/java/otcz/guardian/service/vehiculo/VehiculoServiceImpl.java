package otcz.guardian.service.vehiculo;

import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.DTO.vehiculo.VehiculoConUsuarioResponseDTO;
import otcz.guardian.DTO.vehiculo.VehiculoRegistroDTO;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.entity.vehiculo.VehiculoUsuarioEntity;
import otcz.guardian.repository.vehiculo.VehiculoRepository;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.repository.vehiculo.VehiculoUsuarioRepository;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VehiculoUsuarioRepository vehiculoUsuarioRepository;

    public VehiculoServiceImpl(VehiculoRepository vehiculoRepository, UsuarioRepository usuarioRepository, VehiculoUsuarioRepository vehiculoUsuarioRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.vehiculoUsuarioRepository = vehiculoUsuarioRepository;
    }

    @Override
    public VehiculoEntity registrarVehiculo(VehiculoRegistroDTO vehiculoRegistroDTO) {
        VehiculoEntity vehiculoEntity = new VehiculoEntity();
        vehiculoEntity.setPlaca(vehiculoRegistroDTO.getPlaca());
        vehiculoEntity.setTipo(vehiculoRegistroDTO.getTipo());
        vehiculoEntity.setColor(vehiculoRegistroDTO.getColor());
        vehiculoEntity.setMarca(vehiculoRegistroDTO.getMarca());
        vehiculoEntity.setModelo(vehiculoRegistroDTO.getModelo());
        vehiculoEntity.setActivo(vehiculoRegistroDTO.getActivo());
        vehiculoEntity.setFechaRegistro(java.time.LocalDateTime.now()); // Asignar fecha de registro
        vehiculoEntity = vehiculoRepository.save(vehiculoEntity);

        // Asignar el vehículo al usuario indicado
        Long usuarioId = vehiculoRegistroDTO.getUsuarioId();
        if (usuarioId == null) {
            throw MensajeResponse.usuarioNoEncontradoException();
        }
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(MensajeResponse::usuarioNoEncontradoException);
        boolean yaExiste = !vehiculoUsuarioRepository.findByVehiculo_IdAndUsuario_Id(vehiculoEntity.getId(), usuarioId).isEmpty();
        if (!yaExiste) {
            VehiculoUsuarioEntity relacion = new VehiculoUsuarioEntity();
            relacion.setVehiculo(vehiculoEntity);
            relacion.setUsuario(usuario);
            relacion.setFechaAsignacion(java.time.LocalDateTime.now());
            vehiculoUsuarioRepository.save(relacion);
        }
        return vehiculoEntity;
    }

    @Override
    public VehiculoEntity actualizarVehiculo(VehiculoEntity vehiculoEntity) {
        Optional<VehiculoEntity> existenteOpt = vehiculoRepository.findById(vehiculoEntity.getId());
        if (!existenteOpt.isPresent()) {
            throw new IllegalArgumentException("Vehículo no encontrado");
        }
        VehiculoEntity existente = existenteOpt.get();
        // Conservar la fecha de registro original
        vehiculoEntity.setFechaRegistro(existente.getFechaRegistro());
        return vehiculoRepository.save(vehiculoEntity);
    }

    @Override
    public void eliminarVehiculo(Long id) {
        vehiculoRepository.deleteById(id);
    }

    @Override
    public Optional<VehiculoEntity> obtenerPorPlaca(String placa) {
        return vehiculoRepository.findByPlaca(placa);
    }

    @Override
    public List<VehiculoEntity> listarVehiculosPorUsuario(UsuarioEntity usuarioEntity) {
        // Buscar todas las relaciones donde el usuario esté asociado a un vehículo
        List<VehiculoUsuarioEntity> relaciones = vehiculoUsuarioRepository.findByUsuario_Id(usuarioEntity.getId());
        return relaciones.stream()
                .map(VehiculoUsuarioEntity::getVehiculo)
                .collect(Collectors.toList());
    }

    @Override
    public List<VehiculoEntity> listarVehiculosPorTipo(TipoVehiculo tipo) {
        return vehiculoRepository.findByTipo(tipo);
    }

    @Override
    public List<VehiculoEntity> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity) {
        // Buscar todas las relaciones donde el usuario esté asociado a un vehículo
        List<VehiculoUsuarioEntity> relaciones = vehiculoUsuarioRepository.findByUsuario_Id(usuarioEntity.getId());
        return relaciones.stream()
                .map(VehiculoUsuarioEntity::getVehiculo)
                .filter(VehiculoEntity::getActivo)
                .collect(Collectors.toList());
    }

    @Override
    public void asignarUsuarioAVehiculo(Long vehiculoId, Long usuarioId) {
        VehiculoEntity vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        boolean yaExiste = !vehiculoUsuarioRepository.findByVehiculo_IdAndUsuario_Id(vehiculoId, usuarioId).isEmpty();
        if (yaExiste) {
            throw new IllegalArgumentException("La relación ya existe");
        }
        VehiculoUsuarioEntity relacion = new VehiculoUsuarioEntity();
        relacion.setVehiculo(vehiculo);
        relacion.setUsuario(usuario);
        relacion.setFechaAsignacion(LocalDateTime.now());
        vehiculoUsuarioRepository.save(relacion);
    }

    @Override
    public Optional<VehiculoEntity> obtenerPorId(Long id) {
        return vehiculoRepository.findById(id);
    }

    @Override
    public List<VehiculoEntity> listarTodos() {
        return vehiculoRepository.findAll();
    }

    @Override
    public void eliminarVehiculoPorPlaca(String placa) {
        Optional<VehiculoEntity> vehiculoOpt = vehiculoRepository.findByPlaca(placa);
        if (!vehiculoOpt.isPresent()) {
            throw new IllegalArgumentException(otcz.guardian.DTO.MensajeResponse.VEHICULO_NO_ENCONTRADO.getMensaje());
        }
        try {
            vehiculoRepository.delete(vehiculoOpt.get());
        } catch (Exception e) {
            throw new RuntimeException(otcz.guardian.DTO.MensajeResponse.MENSAJE_GENERICO + " Detalle: " + e.getMessage());
        }
    }

    @Override
    public List<VehiculoConUsuarioResponseDTO> listarVehiculosPorCorreoUsuario(String correo) {
        UsuarioEntity usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        // Buscar todas las relaciones donde el usuario esté asociado a un vehículo
        List<VehiculoUsuarioEntity> relaciones = vehiculoUsuarioRepository.findByUsuario_Id(usuario.getId());
        return relaciones.stream()
                .map(rel -> {
                    VehiculoEntity vehiculo = rel.getVehiculo();
                    VehiculoConUsuarioResponseDTO dto = new VehiculoConUsuarioResponseDTO();
                    dto.setId(vehiculo.getId());
                    dto.setPlaca(vehiculo.getPlaca());
                    dto.setTipo(vehiculo.getTipo());
                    dto.setColor(vehiculo.getColor());
                    dto.setMarca(vehiculo.getMarca());
                    dto.setModelo(vehiculo.getModelo());
                    dto.setActivo(vehiculo.getActivo());
                    dto.setFechaRegistro(vehiculo.getFechaRegistro());
                    // Mapear todos los usuarios asociados a este vehículo
                    List<VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO> usuariosDto = vehiculo.getVehiculoUsuarios().stream()
                            .map(new java.util.function.Function<VehiculoUsuarioEntity, VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO>() {
                                @Override
                                public VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO apply(VehiculoUsuarioEntity vRel) {
                                    UsuarioEntity u = vRel.getUsuario();
                                    VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO usuarioDto = new VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO();
                                    usuarioDto.setId(u.getId());
                                    usuarioDto.setNombreCompleto(u.getNombreCompleto());
                                    usuarioDto.setCorreo(u.getCorreo());
                                    usuarioDto.setTelefono(u.getTelefono());
                                    usuarioDto.setRol(u.getRol() != null ? u.getRol().name() : null);
                                    usuarioDto.setDocumentoIdentidad(u.getDocumentoNumero());
                                    return usuarioDto;
                                }
                            })
                            .collect(Collectors.toList());
                    dto.setUsuarios(usuariosDto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
