FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system tender && useradd --system --gid tender tender
COPY --from=build --chown=tender:tender /app/target/tenderapp-0.0.1-SNAPSHOT.jar app.jar
USER tender
EXPOSE 8080
# DB_* and PAYPAL_* are supplied by Compose or docker run --env-file at runtime.
ENTRYPOINT ["java", "-jar", "app.jar"]
