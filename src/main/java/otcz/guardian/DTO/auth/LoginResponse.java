package otcz.guardian.DTO.auth;

public class LoginResponse {
    private String token;
    private String rol;
    private String correo;

    public LoginResponse(String token, String rol, String correo) {
        this.token = token;
        this.rol = rol;
        this.correo = correo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
