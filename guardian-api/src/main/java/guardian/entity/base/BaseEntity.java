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

    /**
     * Llave del RESIDENTE: enciende o apaga lo suyo. Nunca alcanza para pasar
     * por si sola — ver {@link #puedeOperar()}.
     */
    @Column(name = "ACTIVO", length = 1, nullable = false)
    private String activo;

    /**
     * Llave del ADMINISTRADOR, y solo suya. Gana siempre.
     *
     * <p>Son dos estados independientes a proposito: el residente administra
     * su nucleo, pero cuando la administracion bloquea a alguien o algo, esa
     * decision no se puede revertir desde el celular. Un vehiculo activo pero
     * bloqueado NO sale del conjunto hasta que el administrador lo desbloquee.</p>
     *
     * <p>El default en columna migra las filas existentes como no bloqueadas.</p>
     */
    @Column(name = "BLOQUEADO", length = 1, nullable = false,
            columnDefinition = "varchar(1) default 'N'")
    private String bloqueado;

    @Column(name = "OBSERVACIONES", columnDefinition = "TEXT")
    private String observaciones;

    /** Motivo del bloqueo, para que la porteria pueda explicarlo. */
    @Column(name = "MOTIVO_BLOQUEO", length = 200)
    private String motivoBloqueo;

    @PrePersist
    protected void alCrear() {
        Date ahora = new Date();
        this.fechaCreacion = ahora;
        this.fechaModificacion = ahora;
        if (this.activo == null) {
            this.activo = Codigos.SI;
        }
        if (this.bloqueado == null) {
            this.bloqueado = Codigos.NO;
        }
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaModificacion = new Date();
    }

    /** Solo la llave del residente. Casi nunca es lo que hay que preguntar. */
    public boolean estaActivo() {
        return Codigos.SI.equals(this.activo);
    }

    public boolean estaBloqueado() {
        return Codigos.SI.equals(this.bloqueado);
    }

    /**
     * La pregunta real del sistema: hacen falta LAS DOS llaves. Encendido por
     * su dueno y no bloqueado por la administracion.
     */
    public boolean puedeOperar() {
        return estaActivo() && !estaBloqueado();
    }
}
