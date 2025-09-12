package otcz.guardian.entity.usuario;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class UsuarioDetails implements UserDetails {
    private final UsuarioEntity usuarioEntity;

    public UsuarioDetails(UsuarioEntity usuarioEntity) {
        this.usuarioEntity = usuarioEntity;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(usuarioEntity.getRol().name()));
    }

    @Override
    public String getPassword() {
        return usuarioEntity.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuarioEntity.getCorreo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuarioEntity.getEstado().name().equals("ACTIVO");
    }

    public UsuarioEntity getUsuario() {
        return usuarioEntity;
    }
}

