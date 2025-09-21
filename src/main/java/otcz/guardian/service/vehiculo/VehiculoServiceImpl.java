package otcz.guardian.service.vehiculo;

import otcz.guardian.DTO.vehiculo.VehiculoConUsuarioResponseDTO;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.repository.vehiculo.VehiculoRepository;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.utils.TipoVehiculo;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;

    public VehiculoServiceImpl(VehiculoRepository vehiculoRepository, UsuarioRepository usuarioRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public VehiculoEntity registrarVehiculo(VehiculoEntity vehiculoEntity) {
        return vehiculoRepository.save(vehiculoEntity);
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
        return vehiculoRepository.findByUsuarioEntity(usuarioEntity);
    }

    @Override
    public List<VehiculoEntity> listarVehiculosPorTipo(TipoVehiculo tipo) {
        return vehiculoRepository.findByTipo(tipo);
    }

    @Override
    public List<VehiculoEntity> listarVehiculosActivosPorUsuario(UsuarioEntity usuarioEntity) {
        return vehiculoRepository.findByUsuarioEntityAndActivoTrue(usuarioEntity);
    }

    @Override
    public VehiculoEntity asignarUsuario(Long vehiculoId, Long usuarioId) {
        Optional<VehiculoEntity> vehiculoOpt = vehiculoRepository.findById(vehiculoId);
        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (!vehiculoOpt.isPresent() || !usuarioOpt.isPresent()) {
            throw new IllegalArgumentException("Vehículo o usuario no encontrado");
        }
        VehiculoEntity vehiculo = vehiculoOpt.get();
        vehiculo.setUsuarioEntity(usuarioOpt.get());
        return vehiculoRepository.save(vehiculo);
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
        return vehiculoRepository.findByUsuarioEntity(usuario)
                .stream()
                .map(vehiculo -> {
                    VehiculoConUsuarioResponseDTO dto = new VehiculoConUsuarioResponseDTO();
                    dto.setId(vehiculo.getId());
                    dto.setPlaca(vehiculo.getPlaca());
                    dto.setTipo(vehiculo.getTipo());
                    dto.setColor(vehiculo.getColor());
                    dto.setMarca(vehiculo.getMarca());
                    dto.setModelo(vehiculo.getModelo());
                    dto.setActivo(vehiculo.getActivo());
                    dto.setFechaRegistro(vehiculo.getFechaRegistro());
                    if (vehiculo.getUsuarioEntity() != null) {
                        VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO usuarioDto = new VehiculoConUsuarioResponseDTO.UsuarioSimpleDTO();
                        usuarioDto.setId(vehiculo.getUsuarioEntity().getId());
                        usuarioDto.setNombreCompleto(vehiculo.getUsuarioEntity().getNombreCompleto());
                        usuarioDto.setCorreo(vehiculo.getUsuarioEntity().getCorreo());
                        usuarioDto.setTelefono(vehiculo.getUsuarioEntity().getTelefono());
                        usuarioDto.setRol(vehiculo.getUsuarioEntity().getRol() != null ? vehiculo.getUsuarioEntity().getRol().name() : null);
                        usuarioDto.setDocumentoIdentidad(vehiculo.getUsuarioEntity().getDocumentoNumero());
                        dto.setUsuario(usuarioDto);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
