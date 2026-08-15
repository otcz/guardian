package guardian.service.huella;

import guardian.constant.Codigos;
import guardian.constant.MensajesGlobales;
import guardian.dto.huella.EstadoHuellaResponse;
import guardian.dto.huella.HuellasDeUnaPersonaResponse;
import guardian.dto.huella.RegistrarHuellaRequest;
import guardian.entity.persona.GdHuella;
import guardian.entity.persona.GdPersona;
import guardian.exception.GuardianException;
import guardian.repository.GdHuellaRepository;
import guardian.repository.GdPersonaRepository;
import guardian.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HuellaServiceImpl implements HuellaService {

    /**
     * Tres dedos por persona.
     *
     * <p>No es un limite tecnico sino operativo: cada dedo mas es una
     * comparacion mas por cada persona que llega, y a partir de cierto punto
     * deja de mejorar el acierto. Tres cubre el caso real —una curita, un dedo
     * lastimado, uno que ese dia no lee— sin volver lento el cotejo.</p>
     */
    private static final int MAXIMO_DEDOS = 3;

    /**
     * Tres capturas del MISMO dedo, que el algoritmo funde en UNA plantilla.
     *
     * <p>No son tres huellas. Son tres vistas para que la plantilla aguante que
     * el dedo venga torcido, seco o sucio — que es como llega en una porteria a
     * las seis de la manana.</p>
     */
    private static final int CAPTURAS_POR_DEDO = 3;

    /**
     * Los diez dedos, cada uno identificado por mano y nombre.
     *
     * <p>Antes eran solo DERECHO e IZQUIERDO, y eso no alcanza: si alguien
     * registro el indice y el dia de la curita pone el pulgar, el sistema no
     * tiene forma de decirle cual habia guardado. Nombrar el dedo exacto es lo
     * que permite pedirle "pon el indice derecho" en vez de "pon el de la
     * derecha".</p>
     */
    private static final List<String> DEDOS_VALIDOS = Arrays.asList(
            "IZQ_PULGAR", "IZQ_INDICE", "IZQ_MEDIO", "IZQ_ANULAR", "IZQ_MENIQUE",
            "DER_PULGAR", "DER_INDICE", "DER_MEDIO", "DER_ANULAR", "DER_MENIQUE");

    private final GdHuellaRepository huellaRepository;
    private final GdPersonaRepository personaRepository;
    private final CotejadorHuellas cotejador;

    @Override
    public EstadoHuellaResponse estado() {
        return EstadoHuellaResponse.builder()
                .disponible(cotejador.estaDisponible())
                .algoritmo(cotejador.algoritmo())
                .maximoDedos(MAXIMO_DEDOS)
                .capturasPorDedo(CAPTURAS_POR_DEDO)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HuellasDeUnaPersonaResponse dePersona(Long personaId, UsuarioAutenticado guardia) {
        return mapear(personaDeLaSede(personaId, guardia));
    }

    @Override
    @Transactional
    public HuellasDeUnaPersonaResponse registrar(RegistrarHuellaRequest request,
                                                 UsuarioAutenticado guardia) {
        GdPersona persona = personaDeLaSede(request.getPersonaId(), guardia);
        String dedo = normalizarDedo(request.getDedo());

        if (request.getLecturas().size() != CAPTURAS_POR_DEDO) {
            throw GuardianException.solicitudInvalida(
                    "Se necesitan " + CAPTURAS_POR_DEDO + " lecturas del mismo dedo.");
        }

        Optional<GdHuella> yaExiste = huellaRepository
                .findByPersonaIdAndDedo(persona.getId(), dedo);

        // El tope cuenta los dedos DISTINTOS: volver a tomar uno que ya estaba
        // no consume cupo, lo reemplaza. Quien reintenta lo hace porque el
        // anterior no le funciona.
        if (!yaExiste.isPresent()
                && huellaRepository.countByPersonaId(persona.getId()) >= MAXIMO_DEDOS) {
            throw GuardianException.conflicto(
                    "Esta persona ya tiene " + MAXIMO_DEDOS + " dedos registrados. "
                            + "Borra uno antes de agregar otro.");
        }

        byte[] plantilla = cotejador.fundir(decodificar(request.getLecturas()))
                .orElseThrow(() -> GuardianException.solicitudInvalida(
                        cotejador.estaDisponible()
                                ? "Las lecturas no dieron una huella utilizable. "
                                  + "Vuelve a tomarlas."
                                : "No hay lector de huella configurado."));

        GdHuella huella = yaExiste.orElseGet(GdHuella::new);
        huella.setPersona(persona);
        huella.setDedo(dedo);
        huella.setPlantilla(plantilla);
        huella.setAlgoritmo(cotejador.algoritmo());
        huella.setCalidad(request.getCalidad());
        huella.setActivo(Codigos.SI);
        huella.setBloqueado(Codigos.NO);
        if (huella.getId() == null) {
            huella.setUsuarioCreador(guardia.getDocumento());
        } else {
            huella.setUsuarioModificador(guardia.getDocumento());
        }
        huellaRepository.save(huella);

        // El documento y el dedo, NUNCA la plantilla: un log con datos
        // biometricos es una fuga esperando a ocurrir.
        log.info("[huella] registrada personaId={} dedo={} calidad={} por={}",
                persona.getId(), dedo, request.getCalidad(), guardia.getDocumento());

        return mapear(persona);
    }

    @Override
    @Transactional
    public HuellasDeUnaPersonaResponse eliminar(Long personaId, String dedo,
                                                UsuarioAutenticado guardia) {
        GdPersona persona = personaDeLaSede(personaId, guardia);
        String cual = normalizarDedo(dedo);

        huellaRepository.findByPersonaIdAndDedo(persona.getId(), cual)
                .ifPresent(huella -> {
                    huellaRepository.delete(huella);
                    log.warn("[huella] ELIMINADA personaId={} dedo={} por={}",
                            persona.getId(), cual, guardia.getDocumento());
                });

        return mapear(persona);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * La persona, dentro de la sede de quien pregunta.
     *
     * <p>La frontera importa mas aca que en otros sitios: enrolar la huella de
     * alguien de otra sede le daria paso a un conjunto que no es el suyo.</p>
     */
    private GdPersona personaDeLaSede(Long personaId, UsuarioAutenticado guardia) {
        return personaRepository.findById(personaId)
                .filter(p -> p.getConjunto().getId().equals(guardia.getConjuntoId()))
                .orElseThrow(() -> GuardianException.noEncontrado(
                        MensajesGlobales.NO_ENCONTRADO));
    }

    private String normalizarDedo(String dedo) {
        String limpio = dedo == null ? "" : dedo.trim().toUpperCase();
        if (!DEDOS_VALIDOS.contains(limpio)) {
            throw GuardianException.solicitudInvalida("Ese dedo no existe.");
        }
        return limpio;
    }

    private List<byte[]> decodificar(List<String> lecturas) {
        try {
            return lecturas.stream()
                    .map(l -> Base64.getDecoder().decode(l))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException fallo) {
            throw GuardianException.solicitudInvalida(
                    "Las lecturas no llegaron en un formato valido.");
        }
    }

    private HuellasDeUnaPersonaResponse mapear(GdPersona persona) {
        List<HuellasDeUnaPersonaResponse.DedoRegistrado> dedos =
                huellaRepository.findByPersonaId(persona.getId()).stream()
                        .map(h -> HuellasDeUnaPersonaResponse.DedoRegistrado.builder()
                                .dedo(h.getDedo())
                                .calidad(h.getCalidad())
                                .fechaRegistro(h.getFechaCreacion())
                                .algoritmo(h.getAlgoritmo())
                                .build())
                        .collect(Collectors.toList());

        return HuellasDeUnaPersonaResponse.builder()
                .personaId(persona.getId())
                .nombreCompleto(persona.getNombreCompleto())
                .dedos(dedos)
                .puedeAgregar(dedos.size() < MAXIMO_DEDOS)
                .build();
    }
}
