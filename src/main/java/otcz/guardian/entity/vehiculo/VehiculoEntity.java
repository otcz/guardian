package otcz.guardian.entity.vehiculo;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import otcz.guardian.utils.TipoVehiculo;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "VEHICULOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@EqualsAndHashCode(exclude = "vehiculoUsuarios")
public class VehiculoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false)
    private Long id;

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

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<VehiculoUsuarioEntity> vehiculoUsuarios = new HashSet<>();
}
