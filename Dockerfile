FROM maven:3.9.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY sitepulse-engine-http-api/pom.xml sitepulse-engine-http-api/pom.xml
COPY sitepulse-engine-app/pom.xml sitepulse-engine-app/pom.xml
COPY sitepulse-engine-http-api/src sitepulse-engine-http-api/src
COPY sitepulse-engine-app/src sitepulse-engine-app/src

RUN mvn -q -pl sitepulse-engine-app -am -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /opt/sitepulse-engine

COPY --from=build /workspace/sitepulse-engine-app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
