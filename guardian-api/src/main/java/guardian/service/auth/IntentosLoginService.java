package guardian.service.auth;

/**
 * Freno de fuerza bruta del login, por documento.
 *
 * <p>El riesgo que cubre: el usuario es la cedula y la clave inicial es la
 * misma cedula, asi que sin freno un atacante puede iterar documentos probando
 * documento=clave. El contador vive en memoria (Caffeine): un atacante lento o
 * un reinicio lo evaden, pero encarece el ataque barato — que es el real — sin
 * pagar infraestructura.</p>
 */
public interface IntentosLoginService {

    /**
     * @throws guardian.exception.GuardianException 401 con el mensaje generico
     *         si el documento esta bloqueado. Generico a proposito: confirmar
     *         el bloqueo confirmaria que la cuenta existe.
     */
    void exigirNoBloqueado(String documento);

    void registrarFallo(String documento);

    /** Un login exitoso limpia el contador: el dueno legitimo no arrastra fallos. */
    void limpiar(String documento);
}
