package guardian.service.huella;

import guardian.entity.persona.GdHuella;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * El cotejador mientras no hay lector.
 *
 * <p>Responde que no esta disponible y no hace nada mas. NO lanza excepciones:
 * la porteria tiene que seguir funcionando por codigo y por documento aunque el
 * modulo de huella este vacio, y una excepcion aca tumbaria la pantalla
 * completa por una funcion que todavia no existe.</p>
 *
 * <p>{@code @ConditionalOnMissingBean}: el dia que se agregue el cotejador de
 * verdad, este desaparece solo. No hay que acordarse de borrarlo ni de cambiar
 * una bandera — que es justo el tipo de detalle que se olvida y deja el sistema
 * diciendo "sensor no conectado" con el sensor conectado.</p>
 */
@Slf4j
@Service
@ConditionalOnMissingBean(CotejadorHuellas.class)
public class CotejadorNoConfigurado implements CotejadorHuellas {

    @Override
    public boolean estaDisponible() {
        return false;
    }

    @Override
    public String algoritmo() {
        return "NINGUNO";
    }

    @Override
    public Optional<byte[]> fundir(List<byte[]> lecturas) {
        log.warn("[huella] se intento fundir sin cotejador configurado");
        return Optional.empty();
    }

    @Override
    public Optional<GdHuella> identificar(byte[] leida, List<GdHuella> candidatas) {
        log.warn("[huella] se intento identificar sin cotejador configurado");
        return Optional.empty();
    }
}
