package otcz.guardian.entity.usuario;

import lombok.*;
import javax.validation.constraints.NotNull;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.utils.DocumentoTipo;
import otcz.guardian.utils.EstadoUsuario;
import otcz.guardian.utils.Rol;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "USUARIOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false)
    private Long id;

    @Column(name = "NOMBRE_COMPLETO", nullable = false, length = 100)
    @NotNull
    private String nombreCompleto;

    @Column(name = "CORREO", nullable = false, unique = true, length = 100)
    @NotNull
    private String correo;

    @Column(name = "TELEFONO", nullable = true, length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENTO_TIPO", nullable = false, length = 20)
    @NotNull
    private DocumentoTipo documentoTipo;

    @Column(name = "DOCUMENTO_NUMERO", nullable = false, unique = true, length = 50)
    @NotNull
    private String documentoNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROL", nullable = false, length = 20)
    @NotNull
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    @NotNull
    private EstadoUsuario estado;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    @NotNull
    private String passwordHash;

    @Column(name = "FECHA_REGISTRO", nullable = false)
    @NotNull
    private LocalDateTime fechaRegistro;

    @Column(name = "ULTIMA_CONEXION", nullable = true)
    private LocalDateTime ultimaConexion;

    @Column(name = "CASA", nullable = false, length = 20)
    @NotNull
    private String casa;

    // Relaciones
    @OneToMany(mappedBy = "usuarioEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehiculoEntity> vehiculoEntities;

    @OneToMany(mappedBy = "usuarioEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Invitado> invitados;
}
