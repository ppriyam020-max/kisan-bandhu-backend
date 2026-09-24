# ===== Stage 1: Build the Spring Boot application =====
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the Spring Boot JAR
RUN mvn clean package -DskipTests


# ===== Stage 2: Run the Spring Boot application =====
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the generated JAR
COPY --from=build /app/target/*.jar app.jar

# Spring Boot application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]