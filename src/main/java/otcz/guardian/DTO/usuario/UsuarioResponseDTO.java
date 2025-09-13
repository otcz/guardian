package otcz.guardian.DTO.usuario;

import otcz.guardian.utils.DocumentoTipo;
import otcz.guardian.utils.EstadoUsuario;
import otcz.guardian.utils.Rol;
import java.time.LocalDateTime;

public class UsuarioResponseDTO {
    private Long id;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private DocumentoTipo documentoTipo;
    private String documentoNumero;
    private Rol rol;
    private EstadoUsuario estado;
    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimaConexion;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public DocumentoTipo getDocumentoTipo() { return documentoTipo; }
    public void setDocumentoTipo(DocumentoTipo documentoTipo) { this.documentoTipo = documentoTipo; }
    public String getDocumentoNumero() { return documentoNumero; }
    public void setDocumentoNumero(String documentoNumero) { this.documentoNumero = documentoNumero; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getUltimaConexion() { return ultimaConexion; }
    public void setUltimaConexion(LocalDateTime ultimaConexion) { this.ultimaConexion = ultimaConexion; }
}

