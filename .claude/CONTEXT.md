# CONTEXT.md — Negocio de GUARDIAN

> Reglas de negocio, actores y máquinas de estado del control de acceso.
> Para reglas de arquitectura y código ver [`CLAUDE.md`](CLAUDE.md).

---

## 1. Qué resuelve

Un conjunto residencial necesita saber **quién entra, quién sale y en qué**.
Hoy eso se hace con una minuta de papel: el guardia anota a mano, nadie audita
nada y no hay forma de saber quién está adentro.

GUARDIAN reemplaza la minuta por un registro digital donde cada evento queda
con hora, persona, vehículo, guardia que atendió y punto de acceso.

---

## 2. Actores

| Actor | Entra por | Qué hace |
|---|---|---|
| **Residente** | App móvil (PWA) | Ve su QR, registra sus vehículos |
| **Titular** | App móvil | Lo del residente + administra quién vive en su casa |
| **Invitado** (F2) | Link, sin cuenta | Muestra un QR temporal que le generó la familia, aprobado por el administrador |
| **Guardia** | App de garita | Escanea, verifica la foto, marca a pie/vehículo |
| **Administrador** | Panel web | Habilita y deshabilita casas, personas, vehículos y guardias |

**Parentesco ≠ rol.** `ESPOSO`, `ESPOSA`, `HIJO`, `OTRO` describen la relación
de la persona con la casa; son un atributo de `GD_RESIDENTE_CASA`, no permisos.
Los permisos los da `GD_USUARIO.rol`: `RESIDENTE`, `GUARDIA`, `ADMIN`.

---

## 3. El QR — decisión de diseño

**El QR de residente es permanente y firmado, no rotativo.**

El payload es `GRD1.<codigoPublico>.<HMAC-SHA256>` — no lleva datos personales,
solo un UUID opaco y su firma. Se emite una vez y sirve sin internet en el
teléfono del residente. El QR de invitado usa el prefijo `GRDI` con el mismo
esquema, para que la portería distinga los dos mundos desde el primer byte.

Que sea permanente es una decisión consciente: un QR rotativo obligaría al
residente a tener datos móviles y la app abierta justo cuando llega a la
portería, que es cuando peor señal hay. El riesgo de que alguien fotografíe el
QR y lo reenvíe se cubre en otro lado:

> **La foto en la pantalla del guardia es el control real, no el QR.**
> El QR dice *quién dice ser*; la foto deja que el guardia confirme que la
> persona parada al frente es esa. Sin foto, el sistema es una minuta digital
> más rápida, no un control de acceso.

Por eso `GD_PERSONA.foto_url` es obligatorio antes de emitir credencial, y el
endpoint de verificación siempre devuelve la foto.

Si un QR se compromete, el admin revoca la credencial (`activo = 'N'`) y emite
otra. La revocación es inmediata porque el HMAC se valida contra la fila.

**Invitados (F2) es el caso opuesto**: QR de un solo uso, con vigencia y usos
máximos, generado por la familia. Ahí sí el token es efímero por naturaleza.

**Pero ya no es autoservicio puro.** La invitación nace `PENDIENTE`
(`GD_INVITACION.estado_aprobacion`) y el QR no sirve en la portería hasta que
un administrador la aprueba desde la bandeja de Solicitudes — la ventana de
vigencia empieza a contar igual, pero de nada sirve si nadie aprobó. Antes de
esto el residente invitaba y el código quedaba operativo de inmediato; se
cambió a pedido explícito del dueño del conjunto para que la administración
tenga visibilidad de cada visita antes de que llegue a la puerta.

**Unirse a un hogar con el código del titular tampoco es autoservicio puro.**
Usar el código deja una `GD_SOLICITUD_HOGAR` en `PENDIENTE` con los datos
escritos; la persona, su cuenta y su vínculo con la casa NO existen todavía
— los crea `SolicitudHogarAdminService.aprobar` cuando un administrador
decide, el mismo patrón que ya usaba "pedir casa sin código"
(`GD_SOLICITUD_CASA`). El código del titular no se quema hasta la
aprobación: si se rechaza, sigue sirviendo para un segundo intento.

---

## 4. Flujo de ingreso

```
1. Residente abre la app -> pantalla "Mi QR"
2. Lo muestra en la garita
3. Guardia escanea con la tablet
4. Backend valida: firma HMAC -> credencial activa -> persona activa -> casa activa
5. Pantalla del guardia:
      FOTO grande + nombre + casa + edad + estado + vehículos registrados
6. Guardia elige el modo: [A PIE] o [placa]
7. Se escribe GD_ACCESO_EVENTO (sentido, modo, guardia, punto, resultado)
```

**El sentido lo infiere el sistema**, no lo escoge el guardia: si el último
evento `PERMITIDO` de esa persona fue `ENTRADA`, este es `SALIDA`, y viceversa
(sin ventana de tiempo). El guardia puede corregirlo con un toque explícito —
el flag `corregirSentido` — que queda registrado; un sentido divergente SIN ese
flag se rechaza, porque es una pantalla desactualizada y no una corrección.

**Todo intento se registra, incluso el denegado.** Un QR revocado que se sigue
intentando usar es justo lo que un administrador quiere ver.

---

## 5. Motivos de denegación

Van en el catálogo paramétrico (`MOTIVO_DENEGACION`) porque el conjunto los va
a querer ajustar sin esperar un deploy:

| Código | Cuándo |
|---|---|
| `FIRMA_INVALIDA` | El QR no valida contra el HMAC — probablemente falsificado |
| `CREDENCIAL_REVOCADA` | La credencial existe pero está inactiva |
| `CREDENCIAL_VENCIDA` | Pasó la vigencia (aplica sobre todo a invitados) |
| `PERSONA_INACTIVA` | El admin deshabilitó a la persona |
| `CASA_INACTIVA` | El admin deshabilitó la casa completa |
| `INVITACION_NO_VIGENTE` | La invitación existe pero su ventana aún no empieza |
| `INVITACION_AGOTADA` | La invitación ya consumió todos sus ingresos |
| `INVITACION_PENDIENTE_APROBACION` | El administrador todavía no la aprobó |
| `SIN_CUPO` | Reservado para F4 (control de cupos de parqueadero) |

**Excepción transversal: quien está ADENTRO siempre puede salir.** Una
credencial revocada, una persona inhabilitada o una invitación vencida
bloquean la próxima *entrada*, nunca la salida — retener a alguien dentro del
conjunto no protege a nadie. El evento de salida queda anotado con el estado
de la credencial.

---

## 6. Catálogo paramétrico

Tabla única `GD_PARAMETRO` con `(grupo, codigo, valor, orden, activo)`. Se
siembra desde `GuardianBootstrapInitializer`, **nunca desde scripts SQL**.

Grupos vigentes:

| Grupo | Valores |
|---|---|
| `ROL` | `ADMIN`, `GUARDIA`, `RESIDENTE` |
| `PARENTESCO` | `TITULAR`, `ESPOSO`, `ESPOSA`, `HIJO`, `OTRO` |
| `TIPO_VEHICULO` | `CARRO`, `MOTO`, `BICICLETA`, `OTRO` |
| `MOTIVO_DENEGACION` | los de §5 |
| `TIPO_CREDENCIAL` | `PERMANENTE`, `TEMPORAL` |

**Qué NO es paramétrico y por qué.** `sentido` (`E`/`S`) y `modo`
(`PEATON`/`VEHICULO`) son discriminadores estructurales: el código ramifica
sobre ellos y agregar un tercer valor exigiría escribir lógica nueva de todas
formas. Meterlos al catálogo daría la ilusión de que el admin puede crear un
sentido nuevo, y no puede. Viven en `Codigos.java`.

---

## 7. Autenticación

Usuario = documento de identidad. Clave inicial = el mismo documento, con
`requiereCambioClave = 'S'` forzado hasta que el residente la cambia. El login
compara el usuario sin distinguir mayúsculas.

**Excepción: el administrador sembrado.** Su usuario y clave vienen de
configuración (`GUARDIAN_ADMIN_DOCUMENTO` / `GUARDIAN_ADMIN_CLAVE`, default
`ADMIN` / `230614` en dev). Como la clave la definió quien instaló el sistema
y no es derivable del usuario, entra directo sin cambio forzado; si alguien
configura usuario=clave, el cambio forzado aplica igual que a un residente.

> **Riesgo conocido.** Cualquiera que sepa la cédula de un vecino puede tomar la
> cuenta antes que él. La mitigación en F1 es que **el usuario nace inactivo**:
> el administrador lo habilita cuando entrega el acceso. Sin eso, la puerta de
> entrada del sistema sería más débil que la portería física.
>
> Para F2 se evalúa un código de activación por WhatsApp/SMS que reemplace la
> habilitación manual.

La clave se guarda con BCrypt. El token es JWT HS256 con `usuarioId`, `personaId`
y `rol`, firmado con `GUARDIAN_JWT_SECRET`.

---

## 8. Multi-conjunto

El modelo lleva `GD_CONJUNTO` y todas las tablas de dominio cuelgan de él,
aunque hoy solo haya uno sembrado. Es barato dejarlo ahora y carísimo agregarlo
después: si el producto se vende a un segundo conjunto, el aislamiento ya está
en el modelo y solo falta el filtro por tenant.

En F1 el `conjuntoId` se resuelve del usuario autenticado y no se expone en
ningún endpoint.
