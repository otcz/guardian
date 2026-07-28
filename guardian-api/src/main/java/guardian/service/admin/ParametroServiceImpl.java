package guardian.service.admin;

import guardian.constant.Codigos;
import guardian.dto.common.ParametroResponse;
import guardian.entity.parametro.GdParametro;
import guardian.exception.GuardianException;
import guardian.repository.GdParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParametroServiceImpl implements ParametroService {

    private final GdParametroRepository parametroRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParametroResponse> listarPorGrupo(String grupo) {
        return parametroRepository
                .findByGrupoAndActivoOrderByOrdenAsc(grupo, Codigos.SI)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void exigirCodigoValido(String grupo, String codigo) {
        if (codigo == null || !parametroRepository
                .existsByGrupoAndCodigoAndActivo(grupo, codigo, Codigos.SI)) {
            throw GuardianException.solicitudInvalida(
                    "El valor seleccionado para " + grupo.toLowerCase() + " no es valido.");
        }
    }

    private ParametroResponse mapear(GdParametro parametro) {
        return ParametroResponse.builder()
                .id(parametro.getId())
                .grupo(parametro.getGrupo())
                .codigo(parametro.getCodigo())
                .valor(parametro.getValor())
                .orden(parametro.getOrden())
                .protegido(Codigos.SI.equals(parametro.getProtegido()))
                .build();
    }
}
