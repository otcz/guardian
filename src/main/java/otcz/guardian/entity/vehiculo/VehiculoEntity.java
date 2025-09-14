package otcz.guardian.entity.vehiculo;

import javax.validation.constraints.NotNull;
import lombok.*;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.utils.TipoVehiculo;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VEHICULOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class VehiculoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false)
    private Long id;

    // Propietario del vehículo, obligatorio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    @NotNull
    private UsuarioEntity usuarioEntity;

    @Column(name = "PLACA", nullable = false, unique = true, length = 20)
    @NotNull
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    @NotNull
    private TipoVehiculo tipo;

    @Column(name = "COLOR", nullable = true, length = 30)
    private String color;

    @Column(name = "MARCA", nullable = true, length = 50)
    private String marca;

    @Column(name = "MODELO", nullable = true, length = 50)
    private String modelo;

    @Column(name = "ACTIVO", nullable = false)
    @NotNull
    private Boolean activo;

    @Column(name = "FECHA_REGISTRO", nullable = false)
    @NotNull
    private LocalDateTime fechaRegistro;
}
