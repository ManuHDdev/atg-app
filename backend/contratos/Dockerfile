FROM eclipse-temurin:25-jre
WORKDIR /app
# app.jar se genera en Windows con: .\mvnw.cmd clean package -DskipTests
# y se transfiere al servidor antes de: docker compose build
COPY app.jar .
VOLUME /app/storage
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
