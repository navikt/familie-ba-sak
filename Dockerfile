FROM busybox:1.36.1-uclibc as busybox
# Download opentelemetry-javaagent
# https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/
FROM scratch as javaagent
ARG JAVA_OTEL_VERSION=v1.32.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/$JAVA_OTEL_VERSION/opentelemetry-javaagent.jar /instrumentations/java/javaagent.jar

# Final image
FROM gcr.io/distroless/java21:nonroot
COPY --from=javaagent --chown=nonroot:nonroot /instrumentations/java/javaagent.jar /app/javaagent.jar
COPY --from=busybox /bin/printenv /bin/printenv
COPY --chown=nonroot:nonroot ./target/familie-ba-sak.jar /app/app.jar
WORKDIR /app

ENV APP_NAME=familie-ba-sak
ENV TZ="Europe/Oslo"
# TLS Config works around an issue in OpenJDK... See: https://github.com/kubernetes-client/java/issues/854
ENTRYPOINT [ "java", "-javaagent:/app/javaagent.jar", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/app/app.jar", "-XX:MinRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError" ]

