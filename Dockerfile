# ============================================
# MULTI-STAGE DOCKERFILE PARA SPRING BOOT
# ============================================

# ============================================
# STAGE 1: BUILD
# ============================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copiar archivos de Maven wrapper y pom.xml primero (para aprovechar cache de Docker)
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

# Descargar dependencias (esta capa se cachea si pom.xml no cambia)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Copiar el archivo de credenciales ANTES de compilar
COPY src/main/resources/google-credentials.json /tmp/google-credentials.json

# Compilar la aplicación (sin ejecutar tests para build más rápido)
RUN ./mvnw clean package -DskipTests

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Crear usuario no-root para seguridad
RUN groupadd -r spring && useradd -r -g spring spring

# Instalar curl para healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado desde el stage builder
COPY --from=builder /app/target/*.jar app.jar

# Copiar el archivo de credenciales desde /tmp del stage builder
COPY --from=builder /tmp/google-credentials.json /app/google-credentials.json

# Cambiar permisos
RUN chown spring:spring /app/google-credentials.json

# Cambiar a usuario no-root
USER spring:spring

# Exponer puerto
EXPOSE 8081

# Variables de entorno por defecto (pueden sobrescribirse con docker-compose)
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SERVER_PORT=8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/api/health/ping || exit 1

# Comando de ejecución
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
