package otcz.guardian.controller.Auth;

import otcz.guardian.DTO.MensajeResponse;
import otcz.guardian.DTO.auth.LoginRequest;
import otcz.guardian.DTO.auth.LoginResponse;
import otcz.guardian.entity.usuario.UsuarioEntity;
import otcz.guardian.service.usuario.UsuarioService;
import otcz.guardian.utils.ApiEndpoints;
import otcz.guardian.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RequestMapping(ApiEndpoints.Auth.BASE)
@RestController
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UsuarioService usuarioService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping(ApiEndpoints.Auth.LOGIN)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<UsuarioEntity> usuarioOpt = usuarioService.obtenerUsuarioPorCorreo(loginRequest.getCorreo());

        if (usuarioOpt.isPresent()) {
            UsuarioEntity usuarioEntity = usuarioOpt.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), usuarioEntity.getPasswordHash())) {
                String token = jwtUtil.generateToken(usuarioEntity.getCorreo(), usuarioEntity.getRol().name());
                System.out.println("Token generado: " + token); // Imprime el token en consola
                return ResponseEntity.ok(new LoginResponse(token, usuarioEntity.getRol().name(), usuarioEntity.getCorreo()));
            }
        }
        return ResponseEntity.status(401).body(MensajeResponse.CREDENCIALES_INVALIDAS);
    }
}
