# GUARDIAN

Control de acceso para conjuntos residenciales. Los residentes se identifican en
la portería con un **QR firmado**; el guardia lo escanea y ve en pantalla la
**foto**, el nombre, la casa, la edad y los vehículos registrados antes de
autorizar el ingreso.

---

## Arquitectura

Monolito, a propósito. Un backend y un frontend:

| Componente | Stack | Puerto |
|---|---|---|
| `guardian-api` | Java 8 · Spring Boot 2.7 · PostgreSQL 14 | `8080` |
| `guardian-ui` | Angular 18 · PrimeNG · PWA | `4200` |

El razonamiento de por qué NO son microservicios está en
[`pom.xml`](pom.xml) y en [`.claude/CLAUDE.md`](.claude/CLAUDE.md) §8.

---

## Levantar en local

### 1. Base de datos

```bash
docker compose -f infra/docker-compose.yml up -d postgres
```

Postgres queda en `localhost:5434` (no 5432 ni 5433 para no chocar con otros
proyectos que ya ocupan esos puertos). Usuario `guardian_user`, contraseña
`guardian_pass`, base `guardian`.

### 2. Backend

```bash
cd guardian-api && mvn spring-boot:run
```

Hibernate crea el esquema solo (`ddl-auto=update`) y el
`GuardianBootstrapInitializer` siembra el conjunto, los parámetros y el usuario
administrador. No hay scripts SQL de datos.

Credenciales del admin inicial: documento `1000000000`, clave `1000000000`.
Pide cambio de clave en el primer ingreso.

### 3. Frontend

```bash
cd guardian-ui && npm install && npm start
```

### Todo junto con Docker

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

---

## Estructura

```
guardian/
├── guardian-api/         Backend Spring Boot
│   └── src/main/java/guardian/
│       ├── bootstrap/    Siembra de datos inicial (NO scripts SQL)
│       ├── config/       Beans, CORS, seguridad
│       ├── constant/     ApiEndpoint, MensajesGlobales, Codigos
│       ├── controller/   REST — sin lógica de negocio
│       ├── dto/          Contratos de entrada/salida
│       ├── entity/       @Entity JPA — extienden BaseEntity
│       ├── exception/    Excepciones de dominio + handler global
│       ├── repository/   JpaRepository
│       ├── security/     JWT, filtro, principal autenticado
│       ├── service/      Interface + Impl — toda la lógica vive acá
│       └── util/         Helpers puros
├── guardian-ui/          Frontend Angular PWA
├── infra/                docker-compose, init-db
└── .claude/              Reglas del proyecto para el asistente
```

---

## Estado

**Fase 1 — en curso.** QR de residentes, entrada/salida y panel de
administración.

| Fase | Alcance | Estado |
|---|---|---|
| F1 | Login + cambio de clave, QR residente, entrada/salida, admin | En curso |
| F2 | Invitados con QR temporal + notificación al anfitrión | Pendiente |
| F3 | Huella (biométrico) | Pendiente |
| F4 | Reportes, ocupación en vivo, historial por casa | Pendiente |

El detalle de negocio (actores, reglas, máquina de estados del acceso) está en
[`.claude/CONTEXT.md`](.claude/CONTEXT.md).
