FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r app
COPY --from=build /app/target/billing-backend-1.0.0.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
USER app
EXPOSE 8080
ENTRYPOINT ["sh","/app/docker-entrypoint.sh"]
