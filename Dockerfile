# === Stage 1: Build the application ===
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle bootJar -x test --no-daemon

# === Stage 2: Run the application ===
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port Render will use
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
