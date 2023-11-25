# Download opentelemetry-javaagent
# https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/
FROM scratch as javaagent
ARG JAVA_OTEL_VERSION=v1.32.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/$JAVA_OTEL_VERSION/opentelemetry-javaagent.jar /instrumentations/java/javaagent.jar

# Final image
FROM gcr.io/distroless/java21:nonroot
COPY --from=javaagent --chown=nonroot:nonroot /instrumentations/java/javaagent.jar /app/javaagent.jar
COPY --chown=nonroot:nonroot ./target/familie-ba-sak.jar /app/app.jar
WORKDIR /app
# TLS Config works around an issue in OpenJDK... See: https://github.com/kubernetes-client/java/issues/854
ENTRYPOINT [ "java", "-javaagent:/app/javaagent.jar", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/app/app.jar" ]

ENV APP_NAME=familie-ba-sak