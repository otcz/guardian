package guardian.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ejecuta algo DESPUES de que la transaccion en curso confirme.
 *
 * <p>Es para los efectos que salen del sistema y no se pueden deshacer: borrar
 * un archivo del disco, mandar un correo. Hacerlos en medio de la transaccion
 * es una inconsistencia esperando a ocurrir — si lo que sigue revienta, la base
 * revierte y el efecto ya salio. El correo "tu vehiculo quedo autorizado" de un
 * vehiculo que no existe es peor que no haber avisado.</p>
 *
 * <p>Sin transaccion activa ejecuta directo, para que quien llame no tenga que
 * saber en que contexto esta.</p>
 */
public final class TrasConfirmar {

    private TrasConfirmar() {
    }

    public static void ejecutar(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        accion.run();
                    }
                });
    }
}
