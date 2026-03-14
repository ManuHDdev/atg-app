FROM eclipse-temurin:25-jre
WORKDIR /app
# app.jar se genera en Windows con: .\mvnw.cmd clean package -DskipTests
# y se transfiere al servidor antes de: docker compose build
COPY app.jar .
RUN groupadd -r appuser && useradd -r -g appuser appuser && chown appuser:appuser /app/app.jar
USER appuser
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
