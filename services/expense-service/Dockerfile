FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY services/expense-service/ /workspace/services/expense-service/
COPY common/shared-schema/ /workspace/common/shared-schema/
WORKDIR /workspace/services/expense-service
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/expense-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
