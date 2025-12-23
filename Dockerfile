FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
COPY --chown=nonroot:nonroot ./target/familie-ba-sak.jar /app/app.jar
WORKDIR /app

ENV APP_NAME=familie-ba-sak
ENV TZ="Europe/Oslo"
# TLS Config works around an issue in OpenJDK... See: https://github.com/kubernetes-client/java/issues/854
ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/app/app.jar" ]
