FROM eclipse-temurin:25-jre
WORKDIR /app
# app.jar se genera en Windows con: .\mvnw.cmd clean package -DskipTests
# y se transfiere al servidor antes de: docker compose build
COPY app.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
