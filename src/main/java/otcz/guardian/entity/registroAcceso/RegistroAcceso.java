package otcz.guardian.entity.registroAcceso;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.*;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.entity.usuario.invitado.Invitado;
import otcz.guardian.entity.vehiculo.VehiculoEntity;
import otcz.guardian.utils.ResultadoAcceso;
import otcz.guardian.utils.TipoAcceso;
import java.time.LocalDateTime;

@Entity
@Table(name = "REGISTROS_ACCESO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false)
    private Long id;

    // Usuario que ingresa, opcional si es invitado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USUARIO_ID")
    private UsuarioEntity usuarioEntity;

    // Invitado que ingresa, opcional si es usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVITADO_ID")
    private Invitado invitado;

    // Vehículo asociado, opcional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VEHICULO_ID")
    private VehiculoEntity vehiculoEntity;

    // Guardia que valida el acceso, obligatorio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GUARDIA_ID", nullable = false)
    @NotNull
    private UsuarioEntity guardia;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    @NotNull
    private TipoAcceso tipo; // ENTRADA o SALIDA

    @Column(name = "FECHA_HORA", nullable = false)
    @NotNull
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", nullable = false, length = 20)
    @NotNull
    private ResultadoAcceso resultado;

    /**
     * Validación para asegurar que siempre haya un usuario o un invitado,
     * pero nunca ambos nulos.
     */
    @PrePersist
    @PreUpdate
    private void validarAcceso() {
        if (usuarioEntity == null && invitado == null) {
            throw new IllegalStateException("Debe existir un USUARIO o un INVITADO para el registro de acceso.");
        }
        if (usuarioEntity != null && invitado != null) {
            throw new IllegalStateException("No se puede tener USUARIO e INVITADO al mismo tiempo.");
        }
    }
}
