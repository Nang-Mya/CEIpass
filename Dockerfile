FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY BackEnd/target/*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]