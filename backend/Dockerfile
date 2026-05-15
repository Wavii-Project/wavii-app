# ─────────────────────────────────────────
# Stage 1: Build con Maven
# ─────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Primero copiamos solo el pom.xml para aprovechar caché de capas
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Luego copiamos el código fuente
COPY src ./src

# Build sin tests (los tests se corren en CI)
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────
# Stage 2: Runtime mínimo con JRE 17
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S wavii && adduser -S wavii -G wavii

# Copia el JAR construido
COPY --from=builder /build/target/*.jar app.jar

# Directorio para uploads (documentos de verificación de profesores)
RUN mkdir -p /app/uploads && chown -R wavii:wavii /app

USER wavii

# Puerto de la API
EXPOSE 8080

# Opciones JVM optimizadas para contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
