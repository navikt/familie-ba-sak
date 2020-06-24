# familie-ba-sak
Saksbehandling for barnetrygd

## Kjøring lokalt
For å kjøre opp appen lokalt kan en kjøre `DevLauncher` med Spring-profilen `dev` satt. Dette kan feks gjøres ved å sette
`-Dspring.profiles.active=dev` under Edit Configurations -> VM Options.
Appen tilgjengeliggjøres da på `localhost:8089`. 

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

## Kontaktinformasjon
For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie på slack. Ellers kan man opprette et issue her på github.
