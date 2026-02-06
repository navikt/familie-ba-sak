# Satsendring

Satsendring er en prosess som oppdaterer satser i fagsaker. Dette er nødvendig når det kommer nye satser, og det er
viktig at alle fagsaker har oppdaterte satser for å sikre korrekt utbetaling.


## Smørbrødliste for satsendring
* Skru av toggle familie-ba-sak.satsendring-enablet
    * Må skrus av før neste steg så ikke satsendring automatisk starter før vi rekker å teste eller gi beskjed osv.
        * Denne gjør at satsendring vil bli forsøkt kjørt for fagsaker det ikke er blitt kjørt satsendring på
        * Denne er normalt på, slik at fagsaker som har ligget lenge i GODKJENT-steget/totrinnskontroll blir oppdatert
          etter godkjenning
* Legg inn alle nye satser
    * https://favro.com/organization/98c34fb974ce445eac854de0/1844bbac3b6605eacc8f5543?card=Nav-27182
    * https://github.com/navikt/familie-ba-sak/pull/6012
    * OBS! Alle fagsaker som får nye autovedtak kommer til å få trigget satsendring og så kjøre autovedtaket på nytt
      etter at satsendringen er gjort, SELV OM TOGGLE I PUNKT OVER ER AV
* Gi beskjed til stønadsstatistikk om ny satser og tidspunkt for når vi starter å revurdere alle fagsaker
* Gi beskjed til utbetaling (#utbetaling_ba i slack) om at vi revurderer alle fagsaker pga satsendring
  * Dette er for å forhindre for stor trafikk
* Gi beskjed til linja via fagansvarlige
* Begynn å teste satsendring i preprod
    * Finn fagsaker med forskjellige typer satser og sjekk at satsendringsbehandling blir kjørt ok
    * Trigges ved swagger, se disse
      endepunktene https://familie-ba-sak.intern.dev.nav.no/swagger-ui/index.html?urls.primaryname=forvalter&urls.primaryName=intern#/satsendring-controller
* Teste én sak i prod
* Teste gradvis flere og flere saker i prod
    * Sjekk når konsistensavstemning foregår. Ikke kjør masse saker samtidig som disse, da blir det for mange saker på
      utbetaling.
* Skru på toggle for generell kjøring av satsendring, start med lavt volum (familie-ba-sak.satsendring-hoyt-volum denne
  togglen må være av)
* familie-ba-sak.satsendring-kveld -togglen kan brukes hvis det foretrekkes å kjøre mye saker på kvelden for å blokke SB
  mindre
* familie-ba-sak.satsendring-lordag -togglen kan brukes hvis utbetaling/oppdrag er lørdagsåpent
* Det meste av tools for satsendring skal finnes i swagger (unngå direkte endringer i db pga auditlogg osv)
    * Se satsendring-controller
* Kan være lurt å følge med på dashboard for satsendring i grafana
    * https://grafana.nav.cloud.nais.io/d/eRpQ4Vp7kassa/team-familie-satsendring?orgId=1&from=now-6h&to=now&timezone=browser&var-FssDatasource=000000011&var-miljo=prod&var-gcpDatasource=000000021
    * familie-ba-sak.satsendring-grafana-statistikk - toggle må være på for å få opp data
* Ved hver satsendring pleier det å feile en del fagsaker på at det er “over 100 prosent utbetaling”. Dette er et
  etterslep og fra infotrygd og litt diverse. Bør sendes til fagkyndige som går gjennom manuelt. Ved satsendring feb
  2026 var det 40 saker


## Hvordan satsendring fungerer
Satsendring fungerer ved at det opprettes en ny behandling for hver fagsak som skal få oppdatert satsene. Denne behandlingen kjører gjennom hele autovedtak-prosessen, og oppdaterer satsene i fagsaken. Det er viktig at denne prosessen fungerer korrekt, da det kan påvirke utbetalingene til brukerne.

Når en satsendring er vellykket for en behandling, eller det er sjekket at nye satser er kommet med for en fagsak, vil det legge til en rad i satskjoering-tabellen i databasen. Her vil det også legge seg satskjøringer som feiler, men tilhørende feiltype osv. Anbefaler å gjøre seg godt kjent med denne før man begynner.  
