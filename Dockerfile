FROM ghcr.io/navikt/baseimages/temurin:21

ENV APPD_ENABLED=true
ENV APP_NAME=familie-ba-sak

COPY ./target/familie-ba-sak.jar "app.jar"

