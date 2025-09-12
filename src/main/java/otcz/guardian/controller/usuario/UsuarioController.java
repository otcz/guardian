package otcz.guardian.controller.usuario;


import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.utils.Rol;
import otcz.guardian.utils.EstadoUsuario;
import otcz.guardian.utils.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@RestController
@RequestMapping(ApiEndpoints.Usuario.BASE)
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioEntity> crearUsuario(@RequestBody UsuarioEntity usuarioEntity) {
        usuarioEntity.setPasswordHash(passwordEncoder.encode(usuarioEntity.getPasswordHash()));
        System.out.println(usuarioEntity);
        return ResponseEntity.ok(usuarioService.crearUsuario(usuarioEntity));
    }

    @PutMapping(ApiEndpoints.Usuario.POR_ID)
    public ResponseEntity<UsuarioEntity> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioEntity usuarioEntity) {
        usuarioEntity.setId(id);
        return ResponseEntity.ok(usuarioService.actualizarUsuario(usuarioEntity));
    }

    @DeleteMapping(ApiEndpoints.Usuario.POR_ID)
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ID)
    public ResponseEntity<UsuarioEntity> obtenerUsuarioPorId(@PathVariable Long id) {
        return usuarioService.obtenerUsuarioPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(ApiEndpoints.Usuario.POR_CORREO)
    public ResponseEntity<UsuarioEntity> obtenerUsuarioPorCorreo(@PathVariable String correo) {
        return usuarioService.obtenerUsuarioPorCorreo(correo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ROL)
    public ResponseEntity<List<UsuarioEntity>> listarUsuariosPorRol(@PathVariable Rol rol) {
        return ResponseEntity.ok(usuarioService.listarUsuariosPorRol(rol));
    }

    @GetMapping(ApiEndpoints.Usuario.POR_ESTADO)
    public ResponseEntity<List<UsuarioEntity>> listarUsuariosPorEstado(@PathVariable EstadoUsuario estado) {
        return ResponseEntity.ok(usuarioService.listarUsuariosPorEstado(estado));
    }
}
