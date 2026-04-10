FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM maven:3.9.9-eclipse-temurin-17
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=docker
COPY --from=builder /build/target/backend-0.1.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
