FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY claim-common/pom.xml claim-common/pom.xml
COPY claim-gateway/pom.xml claim-gateway/pom.xml
COPY claim-sign-service/pom.xml claim-sign-service/pom.xml
COPY claim-case-push-service/pom.xml claim-case-push-service/pom.xml
RUN mvn -pl claim-sign-service -am dependency:go-offline
COPY claim-common/src claim-common/src
COPY claim-sign-service/src claim-sign-service/src
RUN mvn -pl claim-sign-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/claim-sign-service/target/claim-sign-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
