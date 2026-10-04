# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/latch-1.0.0-SNAPSHOT.jar /app/latch.jar

EXPOSE 6380
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "/app/latch.jar", "--port", "6380", "--data", "/app/data"]
