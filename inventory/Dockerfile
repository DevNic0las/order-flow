FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY order/pom.xml order/
COPY inventory/pom.xml inventory/
COPY notification/pom.xml notification/
RUN mvn -pl inventory -am dependency:go-offline -q

COPY inventory/src inventory/src
RUN mvn -pl inventory -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/inventory/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]