# familie-ba-sak

// test-kommentar 2

Saksbehandling for barnetrygd

## Kjøring lokalt

For å kjøre opp appen lokalt kan en kjøre

* `DevLauncher`, som kjører opp en H2-(minnebasert) database (obs: kjører med task-rammeverket deaktivert)
* `DevLauncherPostgres`, som kjører opp med Spring-profilen `postgres` satt, og forventer en kjørende database. Samme
  effekt kan du med `DevLauncher` med
  `-Dspring.profiles.active=postgres` satt under `Edit Configurations -> VM Options`.
* `DevLauncherPostgresPreprod`. Kjører mot intergrasjoner og pdl i preprod(ikke q1, men syntetisk). Har støtte for å
  kjøre mot andre miljøer, men da må mock manuelt kommenteres ut i DevLauncherPostgresPreprod. BA_SAK_CLIENT_ID og
  CLIENT_SECRET må settes til familie-ba-sak sin azure client id og secret for å få tilgang til pdl og integrasjoner.
  Frontend må derfor bruke scope mot familie-ba-sak og ikke familie-ba-sak-lokal

Appen tilgjengeliggjøres da på `localhost:8089`. Se [Database](#database) for hvordan du setter opp databasen. For å
tillate kall fra frontend, se [Autentisering](#autentisering).

### Database

#### Embedded database

Bruker du `DevLauncherPostgres`, kan du kjøre opp en embedded database. Da må du sette `--dbcontainer`
under `Edit Configurations -> VM Options`

#### Database i egen container

Postgres-databasen kan settes opp slik:

1. Lag en dockercontainer: 
```
docker run --name familie-ba-sak-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
```
2. List opp alle containerne og finn container id for container med name = familie-ba-sak-postgres: 

```
docker ps
```
3. Kjør docker container: 
```
docker exec -it <container_id> bash
```

4. Åpne postgres som brukeren "postgres":
```
psql -U postgres
```

5. Lag en database med navn "familie-ba-sak": 
```
CREATE DATABASE "familie-ba-sak";
```

Legg til databasen i Intellij: 
1. Trykk på database på høyre side og "+" -> data source -> postgreSQL
2. Fyll inn port=5432, user=postgres, passord=test og database=familie-ba-sak

OBS: Pass på at du ikke kjører postgres lokalt på samme port (5432)


### Autentisering

For å kalle applikasjonen fra fontend må du sette miljøvariablene BA_SAK_CLIENT_ID og CLIENT_SECRET. Dette kan gjøres
under `Edit Configurations -> Environment Variables`. Miljøvariablene kan hentes fra `azuread-familie-ba-sak-lokal` i
dev-gcp-clusteret ved å gjøre følgende:

1. Logg på `gcloud`, typisk med kommandoen: `gcloud auth login`
2. Koble deg til dev-gcp-cluster'et: `kubectl config use-context dev-gcp`
3. Hent info:  
   `kubectl -n teamfamilie get secret azuread-familie-ba-sak-lokal -o json | jq '.data | map_values(@base64d)'`.

BA_SAK_CLIENT_ID må settes til `AZURE_APP_CLIENT_ID` og CLIENT_SECRET til`AZURE_APP_CLIENT_SECRET`

Se `.deploy/nais/azure-ad-app-lokal.yaml` dersom du ønsker å deploye `azuread-familie-ba-sak-lokal`

Til slutt skal miljøvariablene se slik ut:

DevLauncher/DevLauncherPostgres

* BA_SAK_CLIENT_ID=`AZURE_APP_CLIENT_ID` (fra `azuread-familie-ba-sak-lokal`)
* CLIENT_SECRET=`AZURE_APP_CLIENT_SECRET` (fra `azuread-familie-ba-sak-lokal`)

DevLauncherPostgresPreprod:
Trenger i utgangspunktet ikke å sette miljøvariabler manuelt. De hentes automatisk fra Nais.
Krever at man er logget på naisdevice og gcloud.
Husk å sette `BA_SAK_SCOPE=api://dev-gcp.teamfamilie.familie-ba-sak/.default` i `.env`-filen frontend.

Alternativt kan du starte med flagget '--manuellMiljø', og manuelt setje miljøvariablane.
Det krever at man henter azuread fra en pod til familie-ba-sak. Som rulleres oftere enn azuread-familie-ba-sak-lokal
`kubectl -n teamfamilie exec -c familie-ba-sak -it familie-ba-sak-byttmegmedpodid -- env | grep AZURE_APP_CLIENT`

* BA_SAK_CLIENT_ID=`AZURE_APP_CLIENT_ID` (fra `familie-ba-sak`)
* CLIENT_SECRET=`AZURE_APP_CLIENT_SECRET` (fra `familie-ba-sak`)

### Funksjonsbrytere

Vi bruker [unleash](https://unleash.nais.io) til å håndtere funksjonsbrytere.

#### Skru av og på ved lokal testing

Setter du `-D<funksjonsbryter>=[true|false]` på VM Options, vil den gjeldende bryteren skrus av eller på

### Bruke Postman

Du kan bruke Postman til å kalle APIene i ba-sak. Det krever at du har satt opp [autentisering](#autentisering) riktig,
og har et token som gjør at du kaller som ba-sak-frontend.

#### Preprod

Den nødvendige informasjonen for å få frontend-token'et får du ved å kalle:

`kubectl -n teamfamilie get secret azuread-familie-ba-sak-frontend-lokal -o json | jq '.data | map_values(@base64d)'`.

I Postman gjør du et GET-kall med følgende oppsett:

* URL: `https://login.microsoftonline.com/navq.onmicrosoft.com/oauth2/v2.0/token`
* Headers -> Cookie: `fpc=AsRNnIJ3MI9FqfN68mC5KW4`
* Body: `x-www-form-urlencoded` med følgende key-values
    * `grant_type`: `client_credentials`
    * `client_id`: <`AZURE_APP_CLIENT_ID`> fra kubectl-kallet over
    * `client_secret`: <`AZURE_APP_CLIENT_SECRET`> fra kubectl-kallet over
    * `scope`: `api://dev-gcp.teamfamilie.familie-ba-sak-lokal/.default`

<details>
<summary>PROD</summary>
<br>

#### Oppskrift for kall fra Postman mot prod

For å finne den nødvendige informasjonen for å få frontend-token'et i prod må du:

1. Endre kontekst til prod-gcp `kubectl config use-context prod-gcp`
2. Finne navn på secret ved å kjøre `kubectl -n teamfamilie get secrets` og finne navnet på en secret som starter
   med `azure-familie-ba-sak-frontend-`. Kopier navnet på secreten.
3. Kjør `kubectl -n teamfamilie get secret [NAVN PÅ SECRET FRA STEG 2] -o json | jq '.data | map_values(@base64d)'`

I Postman gjør du et GET-kall med følgende oppsett (OBS OBS - husk at dette er rett mot prod!):

* URL: `https://login.microsoftonline.com/navno.onmicrosoft.com/oauth2/v2.0/token`
* Headers -> Cookie: `fpc=AsRNnIJ3MI9FqfN68mC5KW4`
* Body: `x-www-form-urlencoded` med følgende key-values
    * `grant_type`: `client_credentials`
    * `client_id`: <`AZURE_APP_CLIENT_ID`> fra kubectl-kallet over
    * `client_secret`: <`AZURE_APP_CLIENT_SECRET`> fra kubectl-kallet over
    * `scope`: `api://prod-gcp.teamfamilie.familie-ba-sak/.default`

<br>
</details>

#### Lagre token globalt i Postman

Et triks kan være å sette opp en "test" under *Tests* i request'en:

```
pm.test("Lagre token globalt", function () {
    var jsonData = pm.response.json();
    pm.globals.set("azure-familie-ba-sak", jsonData.access_token);
});
```

som vil plukke ut token'et og lagre det i en global variabel, her `azure-familie-ba-sak`

Når du lager kall mot APIet, så kan du sette følgende i header'en for å sende med token'et:

* `Authorization`: `Bearer {{azure-familie-ba-sak}}`

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

Main-branchen blir automatisk bygget ved merge og deployet først til preprod og dernest til prod.

### Hastedeploy

Hvis vi trenger å deploye raskt til prod, har vi egne byggejobber for den biten, som trigges manuelt.

Den ene (krise-rett-i-prod) sjekker ut koden og bygger fra den.

Den andre (krise-eksisterende-image-rett-i-prod) lar deg deploye et tidligere bygd image. Det slår til for eksempel hvis
du skal rulle tilbake til forrige versjon. Denne tar som parameter taggen til imaget du vil deploye. Denne finner du
under actions på GitHub, finn byggejobben du vil gå tilbake til, og kopier taggen derfra.

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

## Les også
* [vilkårsperiodeProdusent - README](src%2Fmain%2Fkotlin%2Fno%2Fnav%2Ffamilie%2Fba%2Fsak%2Fkjerne%2Fvedtak%2Fvedtaksperiode%2Fprodusent%2FREADME.md)

## Kontaktinformasjon

For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie på slack. Ellers kan man opprette et issue
her på github.

## Tilgang til databasene i prod og preprod

Se https://github.com/navikt/familie/blob/master/doc/utvikling/gcp/gcp_kikke_i_databasen.md

