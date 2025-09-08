package otcz.guardian.entity.vehiculo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import otcz.guardian.entity.usuario.Usuario;
import otcz.guardian.utils.TipoVehiculo;
import java.time.LocalDateTime;

@Entity
@Table(name = "VEHICULOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false)
    private Long id;

    // Propietario del vehículo, obligatorio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    @NotNull
    private Usuario usuario;

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
