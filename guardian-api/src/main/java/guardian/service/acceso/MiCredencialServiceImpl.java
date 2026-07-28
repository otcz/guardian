package guardian.service.acceso;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.residente.MiQrResponse;
import guardian.entity.acceso.GdCredencialQr;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.exception.GuardianException;
import guardian.repository.GdCredencialQrRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MiCredencialServiceImpl implements MiCredencialService {

    private final GdPersonaRepository personaRepository;
    private final GdCredencialQrRepository credencialRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;
    private final CredencialQrService credencialQrService;

    @Override
    @Transactional
    public MiQrResponse miQr(UsuarioAutenticado usuario) {
        GdPersona persona = personaRepository.findById(usuario.getPersonaId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        GdCredencialQr credencial = obtenerOEmitir(persona, usuario.getDocumento());

        String casa = residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa)
                .map(c -> c.getIdentificador())
                .orElse(null);

        return MiQrResponse.builder()
                .payload(credencialQrService.construirPayload(credencial))
                .nombreCompleto(persona.getNombreCompleto())
                .documento(persona.getDocumento())
                .casaIdentificador(casa)
                .fotoUrl(persona.getFotoUrl())
                .build();
    }

    @Override
    @Transactional
    public byte[] miQrPng(UsuarioAutenticado usuario, int tamanoPx) {
        GdPersona persona = personaRepository.findById(usuario.getPersonaId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));

        GdCredencialQr credencial = obtenerOEmitir(persona, usuario.getDocumento());
        return credencialQrService.renderizarPng(
                credencialQrService.construirPayload(credencial), tamanoPx);
    }

    private GdCredencialQr obtenerOEmitir(GdPersona persona, String ejecutor) {
        return credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .orElseGet(() -> credencialQrService.emitirPermanente(persona, ejecutor));
    }
}
