package otcz.guardian.DTO.usuario;

import otcz.guardian.utils.DocumentoTipo;
import otcz.guardian.utils.EstadoUsuario;
import otcz.guardian.utils.Rol;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import otcz.guardian.DTO.vehiculo.VehiculoResponseDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Long id;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private DocumentoTipo documentoTipo;
    private String documentoNumero;
    private Rol rol;
    private EstadoUsuario estado;
    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimaConexion;
    private List<VehiculoResponseDTO> vehiculos;
}
