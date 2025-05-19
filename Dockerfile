# Use a base image with Java (e.g., OpenJDK)
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew /app/gradlew
COPY gradle /app/gradle

# Set the execution bit for the Gradle wrapper
RUN chmod +x /app/gradlew

# Copy project files
COPY . /app

# Download dependencies and build the application in one step
RUN /app/gradlew bootJar --no-daemon

# Expose the port your Spring Boot app runs on (usually 8080)
EXPOSE 8080

# Set the command to run the application
ENTRYPOINT ["java", "-jar", "build/libs/*.jar"]
