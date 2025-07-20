# ---- Stage 1: Build using Maven ----
FROM maven:3.8-openjdk-17-slim AS build
LABEL maintainer="raguvaran"

# Set working directory
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the project (skip tests for speed)
RUN mvn clean package -DskipTests

# ---- Stage 2: Runtime using JDK ----
FROM openjdk:17-slim

# Set working directory inside container
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
