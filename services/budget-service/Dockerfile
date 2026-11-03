FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY services/budget-service/ /workspace/services/budget-service/
WORKDIR /workspace/services/budget-service
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/budget-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
