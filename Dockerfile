# Usa una imagen base de Java
FROM openjdk:17-jdk-slim

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia los archivos de Gradle y el código fuente
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src

# Construye el proyecto y genera el .jar
RUN ./gradlew build --no-daemon

# Copia el .jar generado al contenedor
RUN cp build/libs/*.jar app.jar

# Puerto expuesto (ajusta si tu app usa otro puerto)
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]

