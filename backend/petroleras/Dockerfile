FROM eclipse-temurin:25-jre
WORKDIR /app
# app.jar se genera en Windows con: .\mvnw.cmd clean package -DskipTests
# y se transfiere al servidor antes de: docker compose build
COPY app.jar .
# /app/storage debe existir y pertenecer a appuser ANTES de montar el volumen:
# si no, Docker la crea como root al montarlo y el proceso (uid 999) no puede escribir.
RUN groupadd -r appuser && useradd -r -g appuser appuser && mkdir -p /app/storage && chown -R appuser:appuser /app
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
