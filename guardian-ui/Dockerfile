# ============================================================================
# GUARDIAN — frontend
# ============================================================================
# La imagen final NO es solo el build de Angular: es Caddy sirviendo ese build
# Y haciendo de puerta de entrada del stack. Un contenedor menos que separar el
# servidor estatico del proxy, y una sola cosa que reiniciar cuando cambia el
# frontend — que se despliega junto con el backend de todos modos.
# ============================================================================

FROM node:20-alpine AS builder

WORKDIR /build

# Las dependencias en su propia capa: mientras el package-lock no cambie, un
# cambio de codigo no vuelve a bajar node_modules entero.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# La configuracion "production" reemplaza environment.ts por environment.prod.ts,
# donde apiUrl es "/api" — relativo, mismo origen. Por eso no hay que pasarle
# ninguna URL al build: el dominio lo pone quien sirva la pagina.
RUN npm run build -- --configuration production

FROM caddy:2-alpine

# El nombre del directorio de salida lo decide angular.json; se copia el
# contenido de browser/, que es donde Angular 18 deja el bundle del navegador.
# Angular 18 deja el bundle del navegador en browser/, al lado de un server/
# que este proyecto no usa (no hay SSR).
COPY --from=builder /build/dist/guardian-ui/browser /srv

# El Caddyfile NO se copia: lo monta el compose. Asi cambiar una regla del
# proxy no obliga a reconstruir —y por tanto a recompilar— todo el frontend.
