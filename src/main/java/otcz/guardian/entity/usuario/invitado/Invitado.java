package otcz.guardian.entity.usuario.invitado;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import otcz.guardian.entity.usuario.Usuario;
import otcz.guardian.entity.vehiculo.Vehiculo;
import otcz.guardian.utils.DocumentoTipo;
import otcz.guardian.utils.EstadoInvitacion;
import java.time.LocalDateTime;

@Entity
@Table(name = "INVITADOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Invitado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false)
    private Long id;

    // Usuario que invita, obligatorio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    @NotNull
    private Usuario usuario;

    @Column(name = "NOMBRE_COMPLETO", nullable = false, length = 100)
    @NotNull
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENTO_TIPO", nullable = false, length = 20)
    @NotNull
    private DocumentoTipo documentoTipo;

    @Column(name = "DOCUMENTO_NUMERO", nullable = false, length = 50)
    @NotNull
    private String documentoNumero;

    @Column(name = "FECHA_INICIO", nullable = false)
    @NotNull
    private LocalDateTime fechaInicio;

    @Column(name = "FECHA_FIN", nullable = false)
    @NotNull
    private LocalDateTime fechaFin;

    @Column(name = "CODIGO_QR", nullable = false, length = 100, unique = true)
    @NotNull
    private String codigoQR;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    @NotNull
    private EstadoInvitacion estado; // ACTIVA, EXPIRADA, CANCELADA, USADA

    // Vehículo opcional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VEHICULO_ID", nullable = true)
    private Vehiculo vehiculo;

    @Column(name = "FECHA_CREACION", nullable = false)
    @NotNull
    private LocalDateTime fechaCreacion;
}
