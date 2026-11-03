FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY services/group-service/ /workspace/services/group-service/
COPY common/shared-schema/ /workspace/common/shared-schema/
WORKDIR /workspace/services/group-service
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/group-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
