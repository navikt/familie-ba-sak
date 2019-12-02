FROM navikt/java:11-appdynamics

ENV APPD_ENABLED=TRUE

COPY ./target/familie-ba-sak.jar "app.jar"
