FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /code
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
COPY generate-keys.sh .
RUN chmod +x generate-keys.sh && ./generate-keys.sh
RUN mvn package -DskipTests

FROM registry.access.redhat.com/ubi8/openjdk-17:1.20

ENV LANGUAGE='en_US:en'

COPY --from=build /code/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /code/target/quarkus-app/*.jar /deployments/
COPY --from=build /code/target/quarkus-app/app/ /deployments/app/
COPY --from=build /code/target/quarkus-app/quarkus/ /deployments/quarkus/
# Copia as chaves geradas para o local esperado pela imagem de runtime
COPY --from=build /code/src/main/resources/jwt/ /deployments/jwt/

EXPOSE 8080
USER 185
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
