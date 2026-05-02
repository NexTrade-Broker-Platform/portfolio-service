# Stage 1: Build with Gradle
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
# Skip tests to save precious time for your 16:30 demo
RUN gradle build -x test --no-daemon

# Stage 2: Run with JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Adjust the jar name if your project name is different
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]