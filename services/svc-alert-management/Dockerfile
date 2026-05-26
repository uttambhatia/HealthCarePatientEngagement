FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=target/svc-alert-management-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
