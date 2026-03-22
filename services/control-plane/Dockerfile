FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/control-plane-0.1.0-beta.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
