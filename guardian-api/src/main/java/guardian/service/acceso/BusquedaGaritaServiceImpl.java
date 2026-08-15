package guardian.service.acceso;

import guardian.dto.acceso.CandidatoGaritaResponse;
import guardian.entity.conjunto.GdCasa;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdResidenteCasa;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdResidenteCasaRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusquedaGaritaServiceImpl implements BusquedaGaritaService {

    /**
     * Tres letras. Con dos, "an" trae medio conjunto y el guardia tiene que
     * leer una lista larga con gente esperando — que es peor que no ofrecerla.
     */
    private static final int MINIMO_CARACTERES = 3;

    /**
     * Ocho candidatos. No es una limitación técnica: es que si hay más de ocho
     * el guardia no los va a leer, va a pedir el apellido. Un tope corto lo
     * empuja a afinar en vez de a recorrer.
     */
    private static final int MAXIMO_RESULTADOS = 8;

    private final GdPersonaRepository personaRepository;
    private final GdResidenteCasaRepository residenteCasaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CandidatoGaritaResponse> buscar(String texto, UsuarioAutenticado guardia) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() < MINIMO_CARACTERES) {
            // Lista vacía y no un error: esto se llama mientras alguien teclea,
            // y un 400 a mitad de una palabra pinta un error en cada letra.
            return Collections.emptyList();
        }

        List<GdPersona> encontradas = personaRepository.buscarPorNombre(
                guardia.getConjuntoId(), limpio, PageRequest.of(0, MAXIMO_RESULTADOS));

        log.info("[garita] busqueda por nombre '{}' -> {} candidatos (por {})",
                limpio, encontradas.size(), guardia.getDocumento());

        return encontradas.stream().map(this::mapear).collect(Collectors.toList());
    }

    private CandidatoGaritaResponse mapear(GdPersona persona) {
        return CandidatoGaritaResponse.builder()
                .personaId(persona.getId())
                .nombreCompleto(persona.getNombreCompleto())
                .documento(persona.getDocumento())
                .casaIdentificador(casaDe(persona))
                .fotoUrl(persona.getFotoUrl())
                .build();
    }

    /**
     * SIN filtrar por vínculo activo, igual que la verificación del paso: si se
     * filtrara, apagar el vínculo desde el celular haría desaparecer a la
     * persona de esta lista y con ella el bloqueo de su casa. Acá solo sirve
     * para mostrar de qué casa es, pero la regla se mantiene por coherencia con
     * lo que hace el registro.
     */
    private String casaDe(GdPersona persona) {
        return residenteCasaRepository
                .findFirstByPersonaIdOrderByIdAsc(persona.getId())
                .map(GdResidenteCasa::getCasa)
                .map(GdCasa::getIdentificador)
                .orElse(null);
    }
}
