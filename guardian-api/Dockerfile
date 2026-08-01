FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
# Las dependencias en su propia capa: mientras el pom no cambie, un cambio de
# codigo no vuelve a bajar medio Maven Central.
COPY pom.xml .
RUN mvn -q -Dmaven.repo.local=/root/.m2/repository dependency:go-offline

COPY src ./src
# Los tests NO corren aca: los corre el CI en cada push. Repetirlos en el
# build de imagen solo alargaria el deploy.
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
