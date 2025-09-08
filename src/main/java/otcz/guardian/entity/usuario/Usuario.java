package otcz.guardian.entity.usuario;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.utils.DocumentoTipo;
import otcz.guardian.utils.EstadoUsuario;
import otcz.guardian.utils.Rol;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "USUARIOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Usuario {

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

    // Relaciones
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vehiculo> vehiculos;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Invitado> invitados;
}
