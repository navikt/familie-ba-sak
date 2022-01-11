FROM navikt/java:17

ENV APPD_ENABLED=true
ENV APP_NAME=familie-ba-sak

COPY ./target/familie-ba-sak.jar "app.jar"
COPY init.sh /init-scripts/init.sh

