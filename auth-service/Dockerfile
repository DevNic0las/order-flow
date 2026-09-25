FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY order/pom.xml order/
COPY inventory/pom.xml inventory/
COPY notification/pom.xml notification/
COPY auth-service/pom.xml auth-service/
COPY auth-security/pom.xml auth-security/
COPY auth-security/src auth-security/src
COPY gateway/pom.xml gateway/
RUN mvn -pl auth-service -am dependency:go-offline -q

COPY auth-service/src auth-service/src
RUN mvn -pl auth-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
