# familie-ba-sak

Saksbehandling for barnetrygd

## Kjøring lokalt

For å kjøre opp appen lokalt kan en kjøre `DevLauncherPostgres` med Spring-profilen `postgres` satt. Dette kan feks
gjøres ved å sette
`-Dspring.profiles.active=postgres` under `Edit Configurations -> VM Options`. Appen tilgjengeliggjøres da
på `localhost:8089`. Se [databasekapittelet](#database) for hvordan du setter opp databasen. For å tillate kall fra
frontend, se [Autentisering](#autentisering).

Du kan også kjøre med en embedded database. Da må du sette `--dbcontainer` under `Edit Configurations -> VM Options`

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

## Testing i Postman

Det kan være praktisk å teste ut api'et i et verktøy som Postman. Da må vi late som vi er familie-ba-sak-frontend.

Oppsettet består av tre deler:

* Conf'e BA-sak riktig
* Få tak i et gyldig token
* Sende en request med et token'et

### Conf'e BA-sak riktig

Vi trenger å sette tre miljøvariable i familie-ba-sak:

* `BA_SAK_CLIENT_ID`: Id'en til familie-ba-sak
* `CLIENT_SECRET`: Hemmeligheten til familie-ba-sak
* `BA_SAK_FRONTEND_CLIENT_ID`: Id'en til frontend-app'en

Se [Autentisering](#autentisering) for de to første

Verdien for `BA_SAK_FRONTEND_CLIENT_ID` får du tilsvarende med følgende kall:

```
kubectl -n teamfamilie get secret azuread-familie-ba-sak-frontend-lokal -o json | jq '.data | map_values(@base64d)'
```

`AZURE_APP_CLIENT_ID` inneholder client-id'en

### Få tak i et gyldig token (dev-fss)

I postman lagrer du følgende request, som gir deg et token som familie-ba-sak-frontend for å kalle familie-ba-sak:

* Verb: `GET`
* Url: `https://login.microsoftonline.com/navq.onmicrosoft.com/oauth2/v2.0/token`

Under *Body*, sjekk av for `x-www-form-urlencoded`, og legg inn følgende key-value-par:

* `grant_type`: `client_credentials`
* `client_id`: <`AZURE_APP_CLIENT_ID` for familie-ba-sak-frontend>
* `client_secret`: <`AZURE_APP_CLIENT_SECRET` for familie-ba-sak-frontend>
* `scope`: `api://dev-fss.teamfamilie.familie-ba-sak-lokal/.default`

Under *Tests*, legg inn følgende script:

```
pm.test("Lagre token", function () {
    var jsonData = pm.response.json();
    pm.globals.set("azure-familie-ba-sak", jsonData.access_token);
});
```

Lagre gjerne request'en med et hyggelig navn, f.eks 'Token familie-ba-sak-frontend -> familie-ba-sak'

### Sende en request med et token'et

1. `Headers`: Her MÅ du ha `Authorization=Bearer {{azure-familie-ba-sak}}`, altså referanse til token'et som ble lagret
   i scriptet over
2. `Verb`: F.eks `GET`
3. `Url`: F.eks `localhost:8089/api/kompetanse``
   Lagre gjerne request'en med et hyggelig navn, f.eks 'GET kompetanse'

Kjør så:

* 'Token familie-ba-sak-frontend -> familie-ba-sak', for å få token
* 'GET kompetanse' (f.eks) for å gjøre det du VIL gjøre

## Kontaktinformasjon

For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie på slack. Ellers kan man opprette et issue
her på github.

