# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# Cache de dependencias: primero el wrapper y el pom.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Código fuente y empaquetado (los tests corren en CI, no aquí).
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuario no-root.
RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=builder /build/target/api-factusimple.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8090

# Flags JVM contenedor-aware.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
