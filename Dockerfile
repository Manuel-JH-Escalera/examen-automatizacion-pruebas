# ---------- Etapa 1: construcción con Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Primero el pom para cachear dependencias entre builds
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
# El pipeline de CI ya ejecutó las pruebas; aquí solo empaquetamos
RUN mvn -q -B package -DskipTests

# ---------- Etapa 2: imagen de ejecución liviana ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Versión y commit que se inyectan en tiempo de build (los pasa el pipeline)
ARG APP_VERSION=dev
ARG APP_COMMIT=local
ENV APP_VERSION=${APP_VERSION} \
    APP_COMMIT=${APP_COMMIT} \
    PORT=8080

RUN apk add --no-cache curl && adduser -D -u 1001 app
USER app

COPY --from=build /workspace/target/gestor-tareas-*.jar app.jar

EXPOSE 8080

# Health check usado por Docker y el pipeline Blue-Green para decidir si el contenedor nuevo está sano
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
