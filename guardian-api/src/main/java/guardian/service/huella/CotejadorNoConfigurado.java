package guardian.service.huella;

import guardian.entity.persona.GdHuella;
import lombok.extern.slf4j.Slf4j;
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
 * <p><b>Al agregar el cotejador de verdad, marcarlo {@code @Primary}.</b> Sin
 * eso Spring encuentra dos implementaciones de la misma interfaz y no arranca.
 *
 * <p>Aca hubo un {@code @ConditionalOnMissingBean} para que este se retirara
 * solo, y TUMBO LA PRODUCCION: esa anotacion solo funciona dentro de una
 * autoconfiguracion, no sobre una clase que encuentra el escaneo de
 * componentes. Ahi se evalua fuera de orden, esta clase se excluyo a si misma
 * y la aplicacion quedo sin ningun cotejador, en bucle de reinicio. Un
 * {@code @Primary} explicito es feo pero no miente.</p>
 */
@Slf4j
@Service
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
