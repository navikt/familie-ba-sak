# familie-ba-sak

Saksbehandling for barnetrygd

## Kjøring lokalt

For å kjøre opp appen lokalt kan en kjøre `DevLauncherPostgres` med Spring-profilen `postgres` satt. Dette kan feks
gjøres ved å sette
`-Dspring.profiles.active=postgres` under `Edit Configurations -> VM Options`. Appen tilgjengeliggjøres da
på `localhost:8089`. Se [databasekapittelet](#database) for hvordan du setter opp databasen. For å tillate kall fra
frontend, se [Autentisering](#autentisering).

### Database

Dersom man vil kjøre med postgres, kan man bytte til Spring-profilen `postgres`. Da må man sette opp postgres-databasen,
dette gjøres slik:

```
docker run --name familie-ba-sak-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
docker ps (finn container id)
docker exec -it <container_id> bash
psql -U postgres
CREATE DATABASE "familie-ba-sak";
```

Obs! Husk å sette VM Options til `-Dspring.profiles.active=postgres`.

### Autentisering

For å kalle applikasjonen fra fontend må du sette miljøvariablene BA_SAK_CLIENT_ID og CLIENT_SECRET. Dette kan gjøres
under `Edit Configurations -> Environment Variables`. Miljøvariablene kan hentes fra `azuread-familie-ba-sak-lokal` i
dev-gcp clusteret ved å kjøre kommandoen:

`kubectl -n teamfamilie get secret azuread-familie-ba-sak-lokal -o json | jq '.data | map_values(@base64d)'`.

BA_SAK_CLIENT_ID må settes til `AZURE_APP_CLIENT_ID` og CLIENT_SECRET til`AZURE_APP_CLIENT_SECRET`

Se `.deploy/nais/azure-ad-app-lokal.yaml` dersom du ønsker å deploye `azuread-familie-ba-sak-lokal`

Dersom man vil gjøre autentiserte kall mot andre tjenester, må man også legge til scope for den aktuelle tjenesten i
miljøveriablene. Det kan hentes
fra [Vault](https://vault.adeo.no/ui/vault/secrets/kv%2Fpreprod%2Ffss/show/familie-ba-sak/default).

Til slutt skal miljøvariablene se slik ut:

* BA_SAK_CLIENT_ID=`AZURE_APP_CLIENT_ID` (fra `azuread-familie-ba-sak-lokal`)
* CLIENT_SECRET=`AZURE_APP_CLIENT_SECRET` (fra `azuread-familie-ba-sak-lokal`)
* Scope for den aktuelle tjenesten
  (fra [Vault](https://vault.adeo.no/ui/vault/secrets/kv%2Fpreprod%2Ffss/show/familie-ba-sak/default))

### Ktlint

* Vi bruker ktlint i dette prosjektet for å formatere kode.
* Du kan skru på automatisk reformattering av filer ved å installere en plugin som heter`Ktlint (unofficial)`
  fra `Preferences > Plugins > Marketplace`
* Gå til `Preferences > Tools > Actions on Save` og huk av så `Reformat code` og `Optimize imports` er markert.
* Gå til `Preferences > Tools > ktlint`og pass på at `Enable ktlint` og `Lint after Reformat` er huket av.

#### Manuel kjøring av ktlint

* Kjør `mvn antrun:run@ktlint-format` i terminalen

## Kafka

Dersom man vil kjøre med kafka, kan man bytte sette property funksjonsbrytere.vedtak.producer.enabled=true. Da må man
sette opp kafka, dette gjøres gjennom å
kjøre [navkafka-docker-compose](https://github.com/navikt/navkafka-docker-compose) lokal, se README i
navkafka-docker-compose for mer info om hvordan man kjører den.

Topicen vi lytter på må da opprettes via deres api med følgende kommando:

```
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-barnetrygd-vedtak-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 1 }"
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-barnetrygd-saksstatistikk-sak-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 1 }"
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-barnetrygd-saksstatistikk-behandling-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 1 }"

```

## Produksjonssetting

Master-branchen blir automatisk bygget ved merge og deployet til prod.

### Oppretting av Kafka kø

Kafka kø må opprettes manuelt i hver miljø, en gang. Detter gjørs som beskrevet
i [Opprett kø](https://confluence.adeo.no/display/AURA/Kafka#Kafka-NavngivningavTopic)
bruk topic definisjon og konfigurasjon som beskrevet i resources/kafka/topic-<env>.json

## Kontaktinformasjon

For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie på slack. Ellers kan man opprette et issue
her på github.

