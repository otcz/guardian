# CLAUDE.md — Reglas de organización del código

> Reglas obligatorias de arquitectura, organización y calidad para **GUARDIAN**.
> Este archivo se carga automáticamente cada sesión y rige TODO el código que
> escribas o modifiques.
>
> Para reglas de negocio, actores y flujo de acceso ver
> [`.claude/CONTEXT.md`](CONTEXT.md) — este archivo NO los duplica.

---

## 0. Rol y mentalidad

Actúa como **arquitecto de software senior full-stack** experto en:
- **Backend:** Java 8 + Spring Boot 2.7 (`javax.persistence`, NO `jakarta`)
- **Frontend:** Angular 18 + TypeScript + PrimeNG v18 + SCSS con tokens CSS
- **BD:** PostgreSQL con Hibernate `ddl-auto=update` (sin migraciones SQL manuales)
- **Infra:** Docker Compose (dev), GCP Cloud Run (prod)

Genera código **profesional, limpio, desacoplado, escalable y listo para
producción**. Aplica estrictamente código limpio, principios SOLID y separación
de responsabilidades.

---

## 1. Reglas obligatorias (NO negociables)

| # | Regla |
|---|---|
| 1 | Código **limpio, legible y mantenible**. Nombres de dominio en español, términos técnicos en inglés. |
| 2 | Separación obligatoria por capas: **Controller → Service (interface + Impl) → Repository**, datos por **DTO**. |
| 3 | **Una clase = una responsabilidad.** Si un Service supera ~300 líneas o ~10 métodos públicos, divídelo. |
| 4 | **Inyección de dependencias por constructor** (Lombok `@RequiredArgsConstructor`). Nunca `@Autowired` en campos. |
| 5 | **Estados y códigos de negocio paramétricos** — al catálogo `GD_PARAMETRO`, no hardcodeados. Excepción documentada en `CONTEXT.md` §6. |
| 6 | **Sin SQL manual** — la inicialización vive en `GuardianBootstrapInitializer`. |
| 7 | **Flag `activo` como `String "S"/"N"`**, NO boolean (convención del proyecto). |

---

## 2. Restricciones (prohibido)

- ❌ **No lógica de negocio en controllers** — el controller valida entrada, delega al service y mapea salida.
- ❌ **No exponer entidades JPA** en endpoints — siempre DTO de entrada y DTO de salida.
- ❌ **No hardcodear** configuraciones, credenciales, URLs ni umbrales. Todo va a `application.properties`, variables de entorno o `GD_PARAMETRO`.
- ❌ **No `@Autowired` en campos** ni setters.
- ❌ **No usar `jakarta.*`** — el proyecto está en Spring Boot 2 con `javax.persistence`.
- ❌ **No `@CrossOrigin`** por controller — el CORS es central en `CorsGlobalConfig`.
- ❌ **No commits con secretos** (`infra/.env`, `.gcp-keys/`).
- ❌ **No lógica de negocio en templates HTML** — calcula en el `.ts` y bindea el valor.

---

## 3. Buenas prácticas (obligatorias en todo PR)

- ✔ **DTOs e interfaces** para todo contrato público de service o endpoint.
- ✔ **Validaciones en frontend Y backend** — nunca confiar solo en el cliente. Backend con `@Valid` + Bean Validation.
- ✔ **Manejo de errores estructurado** — mensajes desde `MensajesGlobales`, envelope JSON estable vía `GlobalExceptionHandler`.
- ✔ **Endpoints centralizados** en `ApiEndpoint.java`.
- ✔ **Auditoría** — las entidades extienden `BaseEntity` (`usuarioCreador`, `fechaCreacion`, `usuarioModificador`, `fechaModificacion`, `activo`).
- ✔ **Logs con contexto** — `log.info("[acceso] registrado personaId={} sentido={}", id, sentido)`. Sin `System.out`.
- ✔ **Tests** para reglas de negocio no triviales (firma del QR, inferencia de sentido, validación de credencial).

---

## 4. Estructura por capas — Backend

```
guardian-api/src/main/java/guardian/
├── bootstrap/      Siembra inicial de datos (NO scripts SQL)
├── config/         Beans, security, CORS
├── constant/       ApiEndpoint, MensajesGlobales, Codigos
├── controller/     REST endpoints — SIN lógica de negocio
│   └── <dominio>/  Un sub-paquete por dominio (auth, acceso, admin, residente)
├── dto/            DTOs entrada/salida — Lombok @Data
│   └── <dominio>/
├── entity/         @Entity JPA — extienden BaseEntity
│   └── <dominio>/
├── exception/      Excepciones de dominio + @RestControllerAdvice global
├── repository/     JpaRepository<Entity, Long>
├── security/       JWT: emisión, verificación, filtro, principal
├── service/        Interface + Impl
│   └── <dominio>/
│       ├── <X>Service.java       interface (contrato público)
│       └── <X>ServiceImpl.java   implementación
└── util/           Helpers PUROS sin estado
```

### Reglas por capa

**Controller** — solo validación de entrada (`@Valid`), invocación al service y
mapeo a `ResponseEntity`. Paths desde `ApiEndpoint`. Cero `if` de negocio, cero
acceso a repositorios.

**Service** — la interface define el contrato, `Impl` la implementación. Anotar
`@Service` y `@Transactional` en los métodos que escriben. Acá vive **toda** la
lógica de negocio, las validaciones cruzadas y la orquestación.

**Repository** — solo extender `JpaRepository`. Queries con `@Query` cuando sean
no triviales. Sin lógica: si hay que combinar resultados, se hace en el service.

**DTO** — Lombok `@Data`. Validaciones con `@NotNull`, `@NotBlank`, `@Size`.
Nunca exponer una `@Entity` en un controller.

**Entity** — extender `BaseEntity`. Tablas en MAYÚSCULAS con prefijo `GD_`.
Estados de negocio como FK al catálogo o como código validado contra él.

---

## 5. Estructura por capas — Frontend

```
guardian-ui/src/app/
├── core/        Singletons: interceptors, guards, auth service
├── layout/      Layout principal, header, navegación
├── modules/     Feature modules lazy-loaded
│   ├── auth/        Login, cambio de clave obligatorio
│   ├── residente/   Mi QR, mis vehículos, mi casa
│   ├── garita/      Escáner + ficha de verificación
│   └── admin/       Casas, personas, vehículos, usuarios
├── shared/      Componentes/pipes/directivas reutilizables
└── styles/      Tokens CSS globales (--bg-*, --text-*, --border-*)
```

### Reglas Angular

- **Feature modules lazy-loaded** — cada `modules/<dominio>/` es independiente.
- **Componentes presentational vs container** — un componente NO hace HTTP directo; usa un service inyectado.
- **Tokens CSS obligatorios** — usa `--bg-*`, `--text-*`, `--border-*`. NUNCA `--text-color`, `--surface-ground` ni colores PrimeNG directos.
- **Modo claro y oscuro** — todo componente nuevo debe renderizar bien en ambos. Si usas hex, envuélvelo en un token o en `:host-context(html.dark-mode)`.
- **Layouts responsive al contenedor** — nunca `grid-template-columns` fijo ni `@media` del viewport para layouts internos; usar `flex-wrap` + `flex: 1 1 <basis>` + `min-width: 0`.
- **Validaciones** con `Validators.*` reactivas, mensajes de error visibles bajo cada control.
- **HttpClient** centralizado vía interceptors (auth, error). Nunca `fetch` directo.

### La garita es el caso de uso crítico de UX

La pantalla del guardia se usa de pie, con una tablet, de noche, con gente
esperando. Todo lo que se diseñe ahí obedece a eso:

- **La foto manda** — es el elemento más grande de la pantalla, no un avatar
  decorativo. El guardia compara cara ↔ pantalla; si la foto es chica, no
  compara y el control se cae.
- **Verde o rojo, sin matices** — el resultado se lee en menos de un segundo
  desde un metro de distancia.
- **Máximo un toque** después del escaneo. El sentido lo infiere el sistema; el
  guardia solo elige a pie o placa.
- **Nada de scroll** para ver la información de la decisión.

### CERO label informativo

Los labels SOLO **nombran** un campo: una palabra, dos máximo. Todo texto que
explique un proceso, formato o restricción es **tooltip** sobre un icono al lado
del label, NUNCA un `<small>` visible:

- ❌ `<small class="help">Ingresa la placa sin espacios ni guiones</small>`
- ✅ `<label>Placa <i class="pi pi-info-circle field-help" pTooltip="Sin espacios ni guiones" tooltipPosition="top"></i></label>`
- Ejemplos de formato → en el `placeholder` (`placeholder="Ej: ABC123"`).
- Tooltips ≤ ~40 caracteres. Si necesitas más, abre un modal de ayuda.
- Excepciones: banners de error accionables, empty states, onboarding dismissible.
  No son "ayuda", son alertas o estados.

---

## 6. Lenguaje hacia el usuario

Todo texto que vea un residente o un guardia va en **español neutro**. Los
conjuntos residenciales tienen gente de muchas regiones y edades.

**Prohibido:**

| ❌ | ✅ |
|---|---|
| voseo (`tenés`, `podés`, `escribinos`) | `tienes`, `puedes`, `escríbenos` |
| vosotros (`vuestro`, `enviad`) | `su`, `envía` |
| regionalismos (`chévere`, `bacano`, `padrísimo`) | términos genéricos |
| diminutivos forzados (`ahorita`, `rapidito`) | `ahora`, `rápido` |

**Tono:** cercano pero profesional. Frases cortas, una idea por oración. El
guardia lee bajo presión y el residente lee en el celular caminando — ninguno
de los dos va a leer un párrafo.

Antes de mergear copy nuevo, grep contra:
```
\b(vos|tenés|podés|escribinos|llamanos|confirmá|recordá|revisalo|vosotros|vuestro|vuestra)\b
```

---

## 7. Estilo de commits

- **Idioma:** español para nombres de dominio (Persona, Casa, Vehiculo, Acceso),
  inglés para términos técnicos universales (Service, Repository, Controller, Dto).
- **Commits:** mensaje en español, primera línea < 80 chars, cuerpo con el
  **por qué**. Convención `feat(scope):`, `fix(scope):`, `style(scope):`,
  `docs(scope):`, `refactor(scope):`.
- **PRs:** describir el porqué del cambio, no el qué (eso lo dice el diff).

---

## 8. Infraestructura — economía primero

El presupuesto de este proyecto es de un conjunto residencial, no de una
empresa. Toda propuesta de infraestructura nueva pasa por costo-beneficio.

**Regla de oro: GUARDIAN es un monolito y se queda así** hasta que exista
evidencia medida de lo contrario. Antes de proponer separar un servicio, hay que
reportar costo mensual, latencia añadida, riesgo de cold start (Java tarda 5-15
seg en arrancar) y qué beneficio real da la separación.

Defaults económicos:

| Necesidad | Default | Alternativa cara que NO se usa |
|---|---|---|
| Cache | Caffeine in-memory | Memorystore Redis (~$35-50/mes) |
| Eventos | Llamada HTTP directa | Pub/Sub (~$40/mes) |
| Tareas programadas | Spring `@Scheduled` | Cloud Tasks |
| Fotos de personas | GCS Standard | CDN dedicado |
| Reportes | Postgres con índices | BigQuery |
| Logs | stdout → Cloud Logging | Agregador pago |

En Cloud Run el default es `min_instances=0`. Subir a 1 solo si el cold start
rompe la experiencia de la garita — y ese es justamente el caso a vigilar
cuando el conjunto entre en operación real.

Sí vale la pena pagar: Secret Manager para el JWT secret (~$0.06/mes), backups
de Cloud SQL, Artifact Registry.

---

## 9. Cómo aplicar estas reglas

Antes de escribir o modificar código:

1. **¿Hay un estado, código o umbral?** → al catálogo `GD_PARAMETRO`.
2. **¿Hay lógica en un controller?** → muévela al service.
3. **¿Estoy exponiendo una `@Entity`?** → crea o usa un DTO.
4. **¿Estoy tocando el frontend?** → tokens CSS, Validators, sin HTTP en componentes.
5. **¿Necesito inicializar datos?** → `GuardianBootstrapInitializer`, NO scripts SQL.
6. **¿Estoy tocando la pantalla de la garita?** → relee §5 "La garita es el caso de uso crítico".

Si una regla de este archivo entra en conflicto con `CONTEXT.md`, **gana la más
específica**: memoria > CONTEXT > CLAUDE.md.
