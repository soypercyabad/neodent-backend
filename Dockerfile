# =============================================
# ETAPA 1: COMPILAR NEODENTS
# =============================================

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar el archivo de dependencias
COPY pom.xml .

# Descargar dependencias
RUN mvn dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests -B


# =============================================
# ETAPA 2: EJECUTAR SPRING BOOT
# =============================================

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Puerto predeterminado
EXPOSE 8080

# Configuración de memoria de Java
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=10.0 -XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError"

# Ejecutar NeoDents
ENTRYPOINT ["java", "-jar", "app.jar"]