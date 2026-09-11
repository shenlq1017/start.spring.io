# Multi-stage build for start.spring.io China fork
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# Skip tests for image build speed; run tests in CI separately
RUN ./mvnw -pl start-client,start-site -am -DskipTests package -q

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    APPLICATION_OFFLINE=true
COPY --from=build /src/start-site/target/start-site-exec.jar /app/start-site.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/start-site.jar --server.port=${SERVER_PORT} --application.offline=${APPLICATION_OFFLINE}"]
