# familie-ba-sak
Saksbehandling for barnetrygd

## Kjøring lokalt
For å kjøre opp appen lokalt kan en kjøre `DevLauncher` med Spring-profilen `dev` satt. Dette kan feks gjøres ved å sette
`-Dspring.profiles.active=dev` under Edit Configurations -> VM Options.
Appen tilgjengeliggjøres da på `localhost:8089`. 

## Kafka
Dersom man vil kjøre med kafka, kan man bytte til Spring-profilen `kafka-lokal`. Da må man sette opp kafka, dette gjøres gjennom å
kjøre [navkafka-docker-compose](https://github.com/navikt/navkafka-docker-compose) lokal, se README i navkafka-docker-compose for mer info om hvordan man kjører den.

Topicen vi lytter på må da opprettes via deres api med følgende kommando:

```
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-barnetrygd-vedtak-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 3 }"

```

### Database
Dersom man vil kjøre med postgres, kan man bytte til Spring-profilen `postgres`. Da må man sette opp postgres-databasen, dette gjøres slik:
```
docker run --name familie-ba-sak-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
docker ps (finn container id)
docker exec -it <container_id> bash
psql -U postgres
CREATE DATABASE "familie-ba-sak";
```

### Autentisering
Dersom man vil gjøre autentiserte kall mot andre tjenester, må man sette opp følgende miljø-variabler:
* Client secret
* Client id
* Scope for den aktuelle tjenesten

Alle disse variablene finnes i applikasjonens mappe for preprod-fss på vault.
Variablene legges inn under DevLauncher -> Edit Configurations -> Environment Variables. 

## Produksjonssetting
Master-branchen blir automatisk bygget ved merge og deployet til prod.

### Oppretting av Kafka kø
Kafka kø må opprettes manuelt i hver miljø, en gang. Detter gjørs som beskrevet i [Opprett kø](https://confluence.adeo.no/display/AURA/Kafka#Kafka-NavngivningavTopic)
 bruk topic definisjon og konfigurasjon som beskrevet i resources/kafka/topic-<env>.json
  
## Kontaktinformasjon
For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie på slack. Ellers kan man opprette et issue her på github.
