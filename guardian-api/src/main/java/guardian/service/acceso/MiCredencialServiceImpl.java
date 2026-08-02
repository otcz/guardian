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
import guardian.util.FotoUrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        return armar(miPersona(usuario), usuario.getDocumento());
    }

    @Override
    @Transactional
    public MiQrResponse fijarMiFoto(UsuarioAutenticado usuario, String fotoUrl) {
        // Solo una subida de la propia aplicacion. Una URL externa dejaria
        // colar cualquier imagen en el control que el guardia compara.
        if (!FotoUrlUtil.esValida(fotoUrl) || !FotoUrlUtil.tieneFoto(fotoUrl)) {
            throw GuardianException.solicitudInvalida(MensajesGlobales.FOTO_URL_INVALIDA);
        }

        // Su PROPIA persona, del token: sin id en la URL no hay forma de
        // ponerle la cara a otro.
        GdPersona persona = miPersona(usuario);
        persona.setFotoUrl(fotoUrl.trim());
        persona.setUsuarioModificador(usuario.getDocumento());
        personaRepository.save(persona);

        log.info("[residente] foto propia actualizada personaId={}", persona.getId());

        // Con la foto ya puesta, armar() emite la credencial que faltaba.
        return armar(persona, usuario.getDocumento());
    }

    private GdPersona miPersona(UsuarioAutenticado usuario) {
        return personaRepository.findById(usuario.getPersonaId())
                .orElseThrow(() -> GuardianException.noEncontrado(MensajesGlobales.NO_ENCONTRADO));
    }

    /**
     * Sin foto NO se lanza excepcion: se devuelve la ficha con
     * {@code necesitaFoto} y sin payload. Para el residente eso no es un
     * error del sistema sino un paso que le falta, y la pantalla puede
     * ofrecerle resolverlo.
     */
    private MiQrResponse armar(GdPersona persona, String ejecutor) {
        String casa = residenteCasaRepository
                .findFirstByPersonaIdAndActivoOrderByIdAsc(persona.getId(), Codigos.SI)
                .map(GdResidenteCasa::getCasa)
                .map(c -> c.getIdentificador())
                .orElse(null);

        MiQrResponse.MiQrResponseBuilder ficha = MiQrResponse.builder()
                .nombreCompleto(persona.getNombreCompleto())
                .tipoDocumento(persona.getTipoDocumento())
                .documento(persona.getDocumento())
                .casaIdentificador(casa)
                .fotoUrl(persona.getFotoUrl());

        boolean yaTiene = credencialRepository
                .findFirstByPersonaIdAndTipoAndActivoOrderByIdDesc(
                        persona.getId(), Codigos.CREDENCIAL_PERMANENTE, Codigos.SI)
                .isPresent();

        if (!yaTiene && !FotoUrlUtil.tieneFoto(persona.getFotoUrl())) {
            return ficha.necesitaFoto(true).build();
        }

        return ficha
                .payload(credencialQrService.construirPayload(
                        obtenerOEmitir(persona, ejecutor)))
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
