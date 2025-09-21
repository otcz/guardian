package otcz.guardian.entity.catalogo;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "tipo_definicion")
@Getter
@Setter
public class TipoDefinicion {
    @Id
    @Column(name = "tipo_definicion", length = 10)
    private String tipoDefinicion;

    @Column(name = "descripcion_tipo", length = 60)
    private String descripcionTipo;

    @Column(name = "activo", length = 1)
    private String activo;

    @OneToMany(mappedBy = "tipoDefinicion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorDefinicion> valores;
}
