# Etapa 1: Build (Compatible nativamente con linux/arm64 y linux/amd64)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Optimizar memoria de compilación para procesadores ARM (evita OOM en Raspberry Pi)
ENV MAVEN_OPTS="-Xmx1024m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Copiar solo el pom.xml primero para aprovechar la cache de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Empaquetar la aplicación omitiendo los tests
RUN mvn clean package -DskipTests

# Etapa 2: Runtime (Nativo ARM64 linux/arm64v8 con soporte glibc)
# Usar Jammy (Ubuntu glibc) garantiza total compatibilidad con renderizado de fuentes para OpenPDF y Flying Saucer
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Instalar dependencias nativas para generación de PDFs e imágenes (Thumbnailator / Flying Saucer)
RUN apt-get update && \
    apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core && \
    rm -rf /var/lib/apt/lists/*

# Configurar zona horaria
ENV TZ=Europe/Madrid

# Copiar el JAR generado desde la etapa de build
COPY --from=build /app/target/quiropracticoAPI-0.0.1-SNAPSHOT.jar app.jar

# Configurar el punto de entrada optimizado para ARM64 y modo Headless para PDFs
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
