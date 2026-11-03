FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY services/alerts-service/ /workspace/services/alerts-service/
COPY common/shared-schema/ /workspace/common/shared-schema/
WORKDIR /workspace/services/alerts-service
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/alerts-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
