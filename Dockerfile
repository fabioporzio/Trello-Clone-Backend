# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /code
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /work
COPY --from=build /code/target/quarkus-app/ /work/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/work/quarkus-run.jar"]