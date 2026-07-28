FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -Dmaven.test.skip=true clean package

FROM eclipse-temurin:8-jre-jammy

# Usuario no-root: reduce el blast radius si hay escape de container.
# eclipse-temurin no trae un usuario de aplicacion por defecto.
RUN groupadd -r app && useradd -r -g app -d /app -s /sbin/nologin app

WORKDIR /app
COPY --from=builder --chown=app:app /build/target/*.jar app.jar

# Directorio del storage local de fotos (perfil dev). Se crea ACA, antes de
# USER app y con dueno app:app, por dos razones:
#   1. /app pertenece a root; el proceso corre como el usuario app y cualquier
#      escritura ahi moriria con "Permission denied".
#   2. Un volumen montado sobre un path que NO existe en la imagen nace como
#      root:root. Si el path ya existe con el dueno correcto, Docker se lo
#      hereda al inicializarlo.
RUN mkdir -p /data/fotos && chown -R app:app /data

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
