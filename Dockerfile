FROM busybox:1.36.1-uclibc as busybox

# Final image
FROM gcr.io/distroless/java21:nonroot
COPY --from=busybox /bin/printenv /bin/printenv
COPY --chown=nonroot:nonroot ./target/familie-ba-sak.jar /app/app.jar
WORKDIR /app

ENV APP_NAME=familie-ba-sak
ENV TZ="Europe/Oslo"
# TLS Config works around an issue in OpenJDK... See: https://github.com/kubernetes-client/java/issues/854
ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/app/app.jar" ]

