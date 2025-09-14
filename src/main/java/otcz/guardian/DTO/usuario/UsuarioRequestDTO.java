package otcz.guardian.DTO.usuario;

import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDTO {
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String documentoTipo;
    private String documentoNumero;
    private String password;
    private Rol rol;
    private EstadoUsuario estado;
    @NotNull
    private String casa;
}
