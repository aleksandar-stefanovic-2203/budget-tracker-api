FROM eclipse-temurin:27-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:27-jre
WORKDIR /app
COPY --from=build /app/target/budget-tracker-api-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
