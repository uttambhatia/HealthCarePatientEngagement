FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=target/svc-device-ingestion-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
