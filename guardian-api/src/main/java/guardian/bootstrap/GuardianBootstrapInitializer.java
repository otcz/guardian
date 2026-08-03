package guardian.bootstrap;

import guardian.constant.Codigos;
import guardian.entity.conjunto.GdConjunto;
import guardian.entity.parametro.GdParametro;
import guardian.entity.persona.GdPersona;
import guardian.entity.persona.GdUsuario;
import guardian.repository.GdConjuntoRepository;
import guardian.repository.GdParametroRepository;
import guardian.repository.GdPersonaRepository;
import guardian.repository.GdUsuarioRepository;
import guardian.util.PinUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Siembra los datos que el sistema necesita para arrancar. Es la unica fuente
 * de inicializacion: <b>no hay scripts SQL de datos</b> en este proyecto.
 *
 * <p><b>Solo inserta, nunca borra.</b> Cada elemento se crea si no existe, asi
 * que reiniciar la aplicacion es seguro y no pisa cambios del administrador. La
 * contrapartida es que quitar un valor de este archivo no lo borra de la base:
 * eso hay que hacerlo a mano.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuardianBootstrapInitializer implements ApplicationRunner {

    private final GdConjuntoRepository conjuntoRepository;
    private final GdParametroRepository parametroRepository;
    private final GdPersonaRepository personaRepository;
    private final GdUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String EJECUTOR = "SYSTEM";

    /** El default de application.properties. Esta en git: no es un secreto. */
    private static final String CLAVE_PUBLICADA_EN_EL_REPO = "2306";

    @Value("${guardian.bootstrap.super-admin-documento}")
    private String superAdminDocumento;

    @Value("${guardian.bootstrap.super-admin-clave}")
    private String superAdminClave;

    /**
     * Un sistema recien instalado arranca VACIO: solo el catalogo y el super
     * administrador.
     *
     * <p>Antes se sembraba tambien una sede "Conjunto Residencial" con su
     * porteria y su administrador. Eso era de la epoca de un solo conjunto; hoy
     * las sedes las crea el super administrador desde el panel de plataforma, y
     * una sede fantasma sembrada sola aparece en su listado, cuenta como sede
     * real y hay que borrarla a mano de la base.</p>
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sembrarParametros();
        sembrarSuperAdministrador();
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Super administrador de la plataforma.
     *
     * <p>Cuelga de una fila tecnica marcada {@code ES_PLATAFORMA='S'} que NO es
     * una sede: no se lista, no se cuenta y no se puede entrar en ella. Existe
     * porque el vinculo persona-sede es NOT NULL en las bases ya creadas y
     * {@code ddl-auto=update} nunca relaja un NOT NULL.</p>
     */
    private void sembrarSuperAdministrador() {
        if (personaRepository.findByDocumento(superAdminDocumento).isPresent()) {
            return;
        }

        GdConjunto plataforma = conjuntoRepository
                .findFirstByEsPlataforma(Codigos.SI)
                .orElseGet(() -> {
                    GdConjunto fila = new GdConjunto();
                    fila.setNombre("Plataforma GUARDIAN");
                    fila.setEsPlataforma(Codigos.SI);
                    fila.setActivo(Codigos.SI);
                    fila.setUsuarioCreador(EJECUTOR);
                    return conjuntoRepository.save(fila);
                });

        GdPersona persona = new GdPersona();
        persona.setConjunto(plataforma);
        persona.setTipoDocumento(Codigos.TIPO_DOCUMENTO_CC);
        persona.setDocumento(superAdminDocumento);
        persona.setNombres("Super");
        persona.setApellidos("Administrador");
        persona.setActivo(Codigos.SI);
        persona.setUsuarioCreador(EJECUTOR);

        GdPersona guardada = personaRepository.save(persona);

        boolean debilita = claveDemasiadoDebil();

        GdUsuario usuario = new GdUsuario();
        usuario.setPersona(guardada);
        usuario.setRol(Codigos.ROL_SUPER_ADMIN);
        usuario.setClaveHash(passwordEncoder.encode(superAdminClave));
        usuario.setRequiereCambioClave(debilita ? Codigos.SI : Codigos.NO);
        usuario.setActivo(Codigos.SI);
        usuario.setUsuarioCreador(EJECUTOR);
        usuarioRepository.save(usuario);

        if (debilita) {
            log.warn("[bootstrap] super administrador creado usuario={} "
                    + "— la clave sembrada no cumple las reglas del sistema y debe "
                    + "cambiarse en el primer ingreso", superAdminDocumento);
        } else {
            log.info("[bootstrap] super administrador creado usuario={}", superAdminDocumento);
        }
    }

    /**
     * ¿El PIN sembrado pasaria las reglas que el sistema le exige a cualquier
     * persona que elige el suyo?
     *
     * <p>Cuatro casos y los cuatro terminan igual — entrar una vez y cambiarlo:
     * igual al usuario (el que cualquiera adivina), el publicado en el
     * repositorio (equivale a no tener secreto), el que no tiene forma de PIN
     * y el trivial. Los dos ultimos evitan lo mas raro de todo: que el sistema
     * arranque con un PIN que despues no deja escribir, y quien lo cambia no
     * pueda volver a el.</p>
     */
    private boolean claveDemasiadoDebil() {
        return superAdminClave.equals(superAdminDocumento)
                || CLAVE_PUBLICADA_EN_EL_REPO.equals(superAdminClave)
                || !PinUtil.tieneFormaDePin(superAdminClave)
                || PinUtil.esTrivial(superAdminClave);
    }

    private void sembrarParametros() {
        int creados = 0;

        creados += sembrarGrupo(Codigos.GRUPO_ROL, true, Arrays.asList(
                new Opcion(Codigos.ROL_ADMIN, "Administrador"),
                new Opcion(Codigos.ROL_GUARDIA, "Guardia"),
                new Opcion(Codigos.ROL_RESIDENTE, "Residente")));

        creados += sembrarGrupo(Codigos.GRUPO_PARENTESCO, false, Arrays.asList(
                // TITULAR va protegido: la validacion de titular unico por casa
                // lo referencia por codigo y quedaria sin sentido si desaparece.
                new Opcion(Codigos.PARENTESCO_TITULAR, "Titular", true),
                new Opcion("ESPOSO", "Esposo"),
                new Opcion("ESPOSA", "Esposa"),
                new Opcion("HIJO", "Hijo"),
                new Opcion("INVITADO", "Invitado"),
                new Opcion("OTRO", "Otro")));

        // Casa o apartamento. Es lo que antecede al numero en el identificador
        // que lee el guardia — "CASA-101", "APARTAMENTO-301" — asi que los
        // codigos se eligieron legibles: van a salir en pantalla tal cual.
        creados += sembrarGrupo(Codigos.GRUPO_TIPO_VIVIENDA, false, Arrays.asList(
                new Opcion("CASA", "Casa"),
                new Opcion("APARTAMENTO", "Apartamento")));

        creados += sembrarGrupo(Codigos.GRUPO_TIPO_VEHICULO, false, Arrays.asList(
                new Opcion("CARRO", "Carro"),
                new Opcion("MOTO", "Moto"),
                new Opcion("BICICLETA", "Bicicleta"),
                new Opcion("OTRO", "Otro")));

        // Marcas y colores como catalogo y no como texto libre: escritos a
        // mano, el mismo carro entra como "Mazda", "MAZDA" y "mazda", y la
        // busqueda del guardia no encuentra ninguno. Son las marcas que
        // circulan en un conjunto colombiano; el administrador agrega las que
        // le falten desde Configuracion. SIN marcas de bicicleta: el que llega
        // en bici no se identifica por la marca.
        creados += sembrarGrupo(Codigos.GRUPO_MARCA_VEHICULO, false, Arrays.asList(
                new Opcion("CHEVROLET", "Chevrolet"),
                new Opcion("RENAULT", "Renault"),
                new Opcion("MAZDA", "Mazda"),
                new Opcion("TOYOTA", "Toyota"),
                new Opcion("NISSAN", "Nissan"),
                new Opcion("KIA", "Kia"),
                new Opcion("HYUNDAI", "Hyundai"),
                new Opcion("FORD", "Ford"),
                new Opcion("VOLKSWAGEN", "Volkswagen"),
                new Opcion("SUZUKI", "Suzuki"),
                new Opcion("HONDA", "Honda"),
                new Opcion("YAMAHA", "Yamaha"),
                new Opcion("BAJAJ", "Bajaj"),
                new Opcion("AKT", "AKT"),
                new Opcion("OTRA", "Otra")));

        // Colores basicos y distinguibles de noche. Un catalogo largo —"gris
        // grafito", "plata metalizado"— no ayuda al guardia: a esa hora y a esa
        // distancia solo se distingue la familia del color.
        creados += sembrarGrupo(Codigos.GRUPO_COLOR_VEHICULO, false, Arrays.asList(
                new Opcion("BLANCO", "Blanco"),
                new Opcion("NEGRO", "Negro"),
                new Opcion("GRIS", "Gris"),
                new Opcion("PLATA", "Plata"),
                new Opcion("ROJO", "Rojo"),
                new Opcion("AZUL", "Azul"),
                new Opcion("VERDE", "Verde"),
                new Opcion("AMARILLO", "Amarillo"),
                new Opcion("NARANJA", "Naranja"),
                new Opcion("VINOTINTO", "Vinotinto"),
                new Opcion("BEIGE", "Beige"),
                new Opcion("OTRO", "Otro")));

        creados += sembrarGrupo(Codigos.GRUPO_TIPO_CREDENCIAL, true, Arrays.asList(
                new Opcion(Codigos.CREDENCIAL_PERMANENTE, "Permanente"),
                new Opcion(Codigos.CREDENCIAL_TEMPORAL, "Temporal")));

        // Identificacion: en un conjunto hay menores con tarjeta de identidad
        // y extranjeros con cedula de extranjeria o pasaporte. CC va protegido
        // porque es el default estructural de las altas sin tipo.
        creados += sembrarGrupo(Codigos.GRUPO_TIPO_DOCUMENTO, false, Arrays.asList(
                new Opcion(Codigos.TIPO_DOCUMENTO_CC, "Cedula de ciudadania", true),
                new Opcion("TI", "Tarjeta de identidad"),
                new Opcion("CE", "Cedula de extranjeria"),
                new Opcion("PA", "Pasaporte"),
                new Opcion("RC", "Registro civil")));

        creados += sembrarGrupo(Codigos.GRUPO_MOTIVO_DENEGACION, true, Arrays.asList(
                new Opcion(Codigos.MOTIVO_FIRMA_INVALIDA, "Codigo no valido"),
                new Opcion(Codigos.MOTIVO_CREDENCIAL_REVOCADA, "Credencial revocada"),
                new Opcion(Codigos.MOTIVO_CREDENCIAL_VENCIDA, "Credencial vencida"),
                // Dos llaves, dos palabras, y nunca la del otro. "Inactiva" es
                // la del hogar —la apaga el titular desde su celular—;
                // "deshabilitada" es la de la administracion. El guardia lee
                // esto para saber a donde mandar a la persona, y con una sola
                // palabra para las dos causas la manda al sitio equivocado.
                new Opcion(Codigos.MOTIVO_PERSONA_INACTIVA, "Persona inactiva (la apago su hogar)"),
                new Opcion(Codigos.MOTIVO_PERSONA_BLOQUEADA, "Persona deshabilitada por la administracion"),
                new Opcion(Codigos.MOTIVO_CASA_INACTIVA, "Casa inactiva"),
                new Opcion(Codigos.MOTIVO_CASA_BLOQUEADA, "Casa deshabilitada por la administracion"),
                new Opcion(Codigos.MOTIVO_VEHICULO_BLOQUEADO, "Vehiculo deshabilitado por la administracion"),
                new Opcion(Codigos.MOTIVO_VEHICULO_INACTIVO, "Vehiculo inactivo (lo apago su hogar)"),
                new Opcion(Codigos.MOTIVO_VEHICULO_AJENO, "El vehiculo no es de esa casa"),
                new Opcion(Codigos.MOTIVO_INVITACION_NO_VIGENTE, "Invitacion aun no vigente"),
                new Opcion(Codigos.MOTIVO_INVITACION_AGOTADA, "Invitacion sin ingresos disponibles"),
                new Opcion(Codigos.MOTIVO_INVITADO_SIN_VEHICULO, "El invitado no declaro placa"),
                new Opcion(Codigos.MOTIVO_ENTRADA_TRAS_SALIDA, "Intento de reingreso con la credencial vencida")));

        if (creados > 0) {
            log.info("[bootstrap] parametros sembrados nuevos={}", creados);
        }
    }

    private int sembrarGrupo(String grupo, boolean todosProtegidos, List<Opcion> opciones) {
        int creados = 0;
        int orden = 1;

        for (Opcion opcion : opciones) {
            if (!parametroRepository.existsByGrupoAndCodigo(grupo, opcion.codigo)) {
                GdParametro parametro = new GdParametro();
                parametro.setGrupo(grupo);
                parametro.setCodigo(opcion.codigo);
                parametro.setValor(opcion.valor);
                parametro.setOrden(orden);
                parametro.setProtegido(
                        todosProtegidos || opcion.protegido ? Codigos.SI : Codigos.NO);
                parametro.setActivo(Codigos.SI);
                parametro.setUsuarioCreador(EJECUTOR);

                parametroRepository.save(parametro);
                creados++;
            }
            orden++;
        }
        return creados;
    }

    /** Par codigo/valor para sembrar el catalogo sin repetir estructura. */
    private static final class Opcion {
        private final String codigo;
        private final String valor;
        private final boolean protegido;

        private Opcion(String codigo, String valor) {
            this(codigo, valor, false);
        }

        private Opcion(String codigo, String valor, boolean protegido) {
            this.codigo = codigo;
            this.valor = valor;
            this.protegido = protegido;
        }
    }
}
