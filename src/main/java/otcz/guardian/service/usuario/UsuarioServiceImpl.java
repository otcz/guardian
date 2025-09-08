package otcz.guardian.service.usuario;

import otcz.guardian.entity.usuario.Usuario;
import otcz.guardian.repository.usuario.UsuarioRepository;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario crearUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario actualizarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Override
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorDocumento(String documentoNumero) {
        return usuarioRepository.findByDocumentoNumero(documentoNumero);
    }

    @Override
    public List<Usuario> listarUsuariosPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol);
    }

    @Override
    public List<Usuario> listarUsuariosPorEstado(EstadoUsuario estado) {
        return usuarioRepository.findByEstado(estado);
    }
}
