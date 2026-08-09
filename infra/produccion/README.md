# GUARDIAN en el servidor de laboratorio

Despliegue en `jfzambrano` (HP DL380, Ubuntu 26.04). Esta máquina ya corre el
stack de **hermes**, que es de otro proyecto: todo lo de acá está pensado para
convivir con él sin tocarlo.

## Cómo está armado

```
                    Cloudflare Tunnel  ──┐   (cuando exista el dominio)
  tailscale serve :8443 ────────────────┤
                                        ▼
                          127.0.0.1:8090  guardian-web   Caddy + build de Angular
                                            ├── /api/*  →  guardian-api  :8080
                                            └── /*      →  el SPA
                                                           guardian-api
                                                              │
                                                           guardian-db   postgres:14
                                                           guardian-backup
```

Un solo origen: el frontend pide a `/api` relativo, así que no hay CORS ni un
host que mantener sincronizado entre las dos mitades.

## Lo que NO hace

- **Nada escucha en `0.0.0.0`.** El único puerto en el host es `127.0.0.1:8090`.
  Quien expone hacia afuera es el túnel o Tailscale, no Docker.
- **La base no publica puerto.** Se consulta desde el propio servidor:
  ```bash
  docker exec -it guardian-db psql -U guardian -d guardian
  ```
- **No usa el 8080.** En esta máquina, Tailscale Funnel publica el 8080 a
  internet (va al explorador de hermes). Cualquier cosa que se ponga ahí queda
  pública sin haberlo decidido.

## Dónde está cada cosa

| Ruta | Qué hay |
|---|---|
| `/data/guardian/repo` | Checkout del repositorio (deploy key de solo lectura) |
| `/data/guardian/.env` | **Los secretos**, modo 600, fuera de git |
| `/data/guardian/postgres` | Datos de PostgreSQL |
| `/data/guardian/fotos` | Fotos de las personas — dato sensible |
| `/data/guardian/backups` | Volcados diarios, 14 días de retención |

## Primera instalación

```bash
ssh servidor
sudo mkdir -p /data/guardian/{postgres,fotos,backups}
sudo chown -R "$USER" /data/guardian

# El repositorio es privado: la deploy key ya está en ~/.ssh/guardian_deploy
git -c core.sshCommand="ssh -i ~/.ssh/guardian_deploy" \
    clone git@github.com:otcz/guardian.git /data/guardian/repo

cp /data/guardian/repo/infra/produccion/.env.ejemplo /data/guardian/.env
chmod 600 /data/guardian/.env
# Generar los secretos y pegarlos en el .env:
openssl rand -base64 48   # para GUARDIAN_JWT_SECRET
openssl rand -base64 48   # para GUARDIAN_QR_HMAC_SECRET
openssl rand -base64 24   # para GUARDIAN_DB_CLAVE

cd /data/guardian/repo
docker compose --env-file /data/guardian/.env \
  -f infra/produccion/docker-compose.yml up -d --build
```

## Actualizar

```bash
cd /data/guardian/repo
git -c core.sshCommand="ssh -i ~/.ssh/guardian_deploy" pull
docker compose --env-file /data/guardian/.env \
  -f infra/produccion/docker-compose.yml up -d --build
```

El build del backend baja Maven y compila dentro de la imagen; el del frontend
hace lo propio con npm. En esta máquina (48 hilos) tarda poco, y mientras tanto
la versión anterior sigue atendiendo: Compose solo reemplaza el contenedor
cuando la imagen nueva está lista.

## Restaurar un respaldo

El volcado es texto plano comprimido a propósito: se restaura con `psql` en
cualquier máquina, sin depender de que la versión de `pg_restore` coincida.

```bash
gunzip -c /data/guardian/backups/guardian-AAAAMMDD-HHMM.sql.gz \
  | docker exec -i guardian-db psql -U guardian -d guardian
```

> Un respaldo que nunca se restauró no es un respaldo. Conviene probarlo contra
> una base desechable antes de necesitarlo de verdad.

## Publicar hacia afuera

**Hoy, dentro del tailnet** — da HTTPS de verdad, que hace falta para que el
service worker de la PWA se registre (exige contexto seguro):

```bash
tailscale serve --bg --https=8443 http://127.0.0.1:8090
```

Queda en `https://jfzambrano.tailb456ca.ts.net:8443`, visible solo para los
equipos del tailnet. `serve` **no** es `funnel`: no publica nada a internet.

**Cuando exista el dominio** — Cloudflare Tunnel, porque el servidor está
detrás de NAT (`192.168.100.246`, salida `138.0.89.38`) y no se puede apuntar
un dominio a esa IP sin abrir puertos en el router de la oficina:

```bash
cloudflared tunnel login
cloudflared tunnel create guardian
cloudflared tunnel route dns guardian guardian.TU-DOMINIO.com
# ingress: guardian.TU-DOMINIO.com -> http://127.0.0.1:8090
```

Después, agregar ese hostname a `GUARDIAN_ALLOWED_ORIGINS` en el `.env` y
reiniciar `guardian-api`.
