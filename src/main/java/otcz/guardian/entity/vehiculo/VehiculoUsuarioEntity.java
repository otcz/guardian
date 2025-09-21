package otcz.guardian.entity.vehiculo;

import lombok.*;
import otcz.guardian.entity.usuario.UsuarioEntity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculo_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(VehiculoUsuarioEntity.PK.class)
public class VehiculoUsuarioEntity implements Serializable {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private VehiculoEntity vehiculo;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    // Ejemplo de campo adicional
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long vehiculo;
        private Long usuario;
    }
}

