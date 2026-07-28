package guardian.entity.base;

import guardian.constant.Codigos;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Campos de auditoria heredados por todas las entidades del dominio.
 *
 * <p>El flag {@code activo} es String "S"/"N" y no boolean por convencion del
 * proyecto: en un dominio donde nada se borra fisicamente, un tercer estado
 * futuro (por ejemplo "suspendido") cabe sin migrar la columna.</p>
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "USUARIO_CREADOR", length = 60)
    private String usuarioCreador;

    @Column(name = "FECHA_CREACION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;

    @Column(name = "USUARIO_MODIFICADOR", length = 60)
    private String usuarioModificador;

    @Column(name = "FECHA_MODIFICACION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaModificacion;

    @Column(name = "ACTIVO", length = 1, nullable = false)
    private String activo;

    @Column(name = "OBSERVACIONES", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    protected void alCrear() {
        Date ahora = new Date();
        this.fechaCreacion = ahora;
        this.fechaModificacion = ahora;
        if (this.activo == null) {
            this.activo = Codigos.SI;
        }
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaModificacion = new Date();
    }

    public boolean estaActivo() {
        return Codigos.SI.equals(this.activo);
    }
}
