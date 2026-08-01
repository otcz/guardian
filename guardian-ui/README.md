# guardian-ui

Frontend PWA de **GUARDIAN** — control de acceso residencial. Angular 18 +
PrimeNG, con tres paneles por rol:

| Ruta | Panel | Quién |
|---|---|---|
| `/app` | Mi QR, Mi hogar, Invitados | Residente / Titular |
| `/porteria` | Escáner y ficha de verificación | Guardia |
| `/admin` | Resumen, casas, personas, vehículos, usuarios, invitaciones, bitácora | Administrador |
| `/invitado/:codigo` | Página pública del QR temporal | Invitado (sin cuenta) |

## Correr en local

```bash
npm install
npm start        # http://localhost:4200
```

El backend debe estar arriba en `http://localhost:8484` (ver el README de la
raíz del repo). La URL del API vive en `src/environments/`.

## Convenciones

Las reglas de arquitectura del frontend — tokens CSS, lazy loading, cero HTTP
en componentes, modo claro/oscuro, la pantalla de la garita — están en
[`../.claude/CLAUDE.md`](../.claude/CLAUDE.md) §5 y son obligatorias.

```bash
npm test         # Karma + Jasmine
npm run build    # produccion, con service worker (PWA)
```
