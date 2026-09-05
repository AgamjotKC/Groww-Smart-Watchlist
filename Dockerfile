# Stage 1: Build application using Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and dependencies configuration
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Copy source code and build executable JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal Lightweight Runtime Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built JAR artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose HTTP application port
EXPOSE 8080

# Set Indian Standard Time timezone for market session alignment
ENV TZ=Asia/Kolkata

# Run Spring Boot Application
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]
