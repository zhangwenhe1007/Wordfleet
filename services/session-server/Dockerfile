FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/session-server-0.1.0-beta.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
