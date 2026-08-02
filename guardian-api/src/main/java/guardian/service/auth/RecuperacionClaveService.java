package guardian.service.auth;

import guardian.dto.auth.RestablecerClaveRequest;
import guardian.dto.auth.SolicitarCodigoRequest;
import guardian.dto.auth.SolicitudCodigoResponse;

public interface RecuperacionClaveService {

    /**
     * Paso 1: emite un codigo y lo manda al correo de la persona.
     *
     * <p>NUNCA falla por "no existe". Responde lo mismo para un documento real
     * y para uno inventado: si distinguiera, esta pantalla —abierta sin
     * sesion— seria un verificador de que cedulas viven en el conjunto.</p>
     */
    SolicitudCodigoResponse solicitar(SolicitarCodigoRequest request);

    /**
     * Paso 2: valida el codigo y cambia la contrasena.
     *
     * <p>Aca SI falla con mensaje, pero con UNO SOLO para codigo equivocado,
     * vencido, ya usado y documento inexistente. Distinguirlos le diria a quien
     * prueba codigos cual de las cuatro cosas acerto.</p>
     */
    void restablecer(RestablecerClaveRequest request);
}
