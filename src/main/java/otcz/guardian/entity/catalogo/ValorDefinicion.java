package otcz.guardian.entity.catalogo;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "valor_definicion")
@Getter
@Setter
public class ValorDefinicion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "valor_definicion_id")
    private Long valorDefinicionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_definicion", referencedColumnName = "tipo_definicion", nullable = false)
    private TipoDefinicion tipoDefinicion;

    @Column(name = "valor_definicion", length = 60, nullable = false)
    private String valorDefinicion;

    @Column(name = "descripcion_valor_definicion", length = 200)
    private String descripcionValorDefinicion;

    @Column(name = "activo", length = 1)
    private String activo;
}
