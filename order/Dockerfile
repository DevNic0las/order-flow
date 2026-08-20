FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY order/pom.xml order/
COPY inventory/pom.xml inventory/
COPY notification/pom.xml notification/
COPY order/src order/src
COPY auth/pom.xml auth/
COPY auth/src auth/src
RUN mvn -pl order -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/order/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]