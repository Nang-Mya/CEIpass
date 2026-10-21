FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY BackEnd/target/cei-pass-api-0.1.0-SNAPSHOT.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]