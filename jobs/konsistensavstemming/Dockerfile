FROM gcr.io/distroless/java21
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY ./target/familie-ba-konsistensavstemming.jar /app/app.jar
USER nonroot
CMD ["app.jar"]