FROM eclipse-temurin:17-jre
RUN groupadd --system --gid 10001 appuser && useradd --system --uid 10001 --gid 10001 --create-home appuser
WORKDIR /app
ARG JAR_FILE=target/svc-patient-0.1.0-SNAPSHOT.jar
COPY --chown=10001:10001 ${JAR_FILE} app.jar
USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
