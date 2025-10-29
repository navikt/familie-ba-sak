# Tjeneste for Pensjon

Erstatter gammel rutine for overføring av barnetrygddata (fra Infotrygd via fil) ifm. med godskrivning av pensjonsopptjening for mottakere av barnetrygd foregående år.

Ble i utgangspunktet kun laget for henting av data fra familie-ba-sak, da man så for seg infotrygdsakene fortsatt ville komme fra gammel løype, men er på forespørsel utvidet til å hente data fra Infotrygd (via [familie-ba-infotrygd](https://github.com/navikt/familie-ba-infotrygd)) i tillegg.

Domenemodellen er derfor primært tilpasset ba-sak, med høyere detaljnivå enn det man kan tilby med dataene fra Infotrygd, men det er begrensninger som er innforstått (ref. [slack](https://nav-it.slack.com/archives/C04JGFS9A3E/p1692275651280949?thread_ts=1692272606.816909&cid=C04JGFS9A3E))

## Oppbygning

### Steg 1 - Innhenting av identer

Pensjon starter med å bestille overlevering av alle identer med barnetrygd innenfor et gitt år ved å kalle endepunktet [/api/ekstern/pensjon/bestill-personer-med-barnetrygd/{år}](https://familie-ba-sak.intern.dev.nav.no/swagger-ui/index.html#/pensjon-controller/bestillPersonerMedBarnetrygdForGitt%C3%85rP%C3%A5Kafka) under [PensjonController](PensjonController.kt), som oppretter en [HentAlleIdenterTilPsysTask](../../task/HentAlleIdenterTilPsysTask.kt)

Denne finner først alle identer fra ba-sak med spørringen gitt av `finnIdenterMedLøpendeBarnetrygdForGittÅr` i [AndelTilkjentYtelseRepository](../../kjerne/beregning/domene/AndelTilkjentYtelseRepository.kt), og deretter alle identer fra Infotrygd via integrasjonen mot [/infotrygd/barnetrygd/pensjon](https://familie-ba-infotrygd.intern.dev.nav.no/swagger-ui/index.html#/pensjon-controller/personerMedBarnetrygd) i [InfotrygdBarnetrygdKlient](../../integrasjoner/infotrygd/InfotrygdBarnetrygdKlient.kt). Tasken avslutter så med å publisere alle unike ident-forekomster til kafka-topic'en med navn [aapen-familie-ba-sak-identer-med-barnetrygd](../../../../../../../../../../.nais/kafka/aapen-familie-ba-sak-identer-med-barnetrygd-dev.yaml).

I dev-miljø er det en [funksjonsbryter](https://teamfamilie-unleash-web.iap.nav.cloud.nais.io/projects/default/features/familie-ba-sak.hent-identer-til-psys-fra-infotrygd) som styrer om det skal inkluderes data fra Infotrygd 
eller ikke. Det er fordi det returneres ekte fødselsnumre derfra, siden `infotrygd_baq` basen stort sett bare er en kopi av `infotrygd_bap`. Den bør helst være avskrudd med mindre Pensjon etterlyser å få teste med bryteren på, siden kjøringer med sensitive data kun gjøres fra et bestemt testmiljø.

### Steg 2 - Innhenting av perioder

Hver kafka-melding av type `DATA` publisert i steg 1, vil følges opp med et POST-kall for henting av perioder til [/api/ekstern/pensjon/hent-barnetrygd](https://familie-ba-sak.intern.dev.nav.no/swagger-ui/index.html#/pensjon-controller/hentBarnetrygd) med body på format:
```
{
    "ident": <ident-fra-kafka-melding>,
    "fraDato": "2023-01-01" // med samme år som i steg 1
}
```

Responsen er på format

```
{
  "fagsaker": [
    {
      "fagsakEiersIdent": <søkers ident>,
      "barnetrygdPerioder": [
        {
          "personIdent": <barnets ident>,
          "delingsprosentYtelse": "USIKKER",
          "ytelseTypeEkstern": "ORDINÆR_BARNETRYGD",
          "utbetaltPerMnd": 321,
          "stønadFom": "2023-01",
          "stønadTom": "2023-09",
          "sakstypeEkstern": "EØS",
          "kildesystem": "Infotrygd",
          "pensjonstrygdet": true,       // settes kun fra Infotrygd
          "norgeErSekundærland": null    // settes kun fra ba-sak
        }
      ]
    }
  ]
}
```
hvor det returneres en liste av fagsaker med tilhørende barnetrygdperioder fra den aktuelle datoen. Grunnen til at det er en liste er fordi det også returneres periodedata fra relaterte fagsaker via barna på identen

Akkurat som i steg 1, hentes det også her data fra både infotrygd og ba-sak. Dersom det finnes perioder med overlappende dato fra forskjellig kildesystem, er det ba-sak periodene som skal gjelde (ref [slack](https://nav-it.slack.com/archives/C04JGFS9A3E/p1700725524272959?thread_ts=1700650664.696339&cid=C04JGFS9A3E)). Filtreringen gjøres vha. tidslinjer med funksjonen `fjernOverlappendeInfotrygdperioder` i [PensjonService](PensjonService.kt)

Dersom [funksjonsbryteren](https://teamfamilie-unleash-web.iap.nav.cloud.nais.io/projects/default/features/familie-ba-sak.hent-identer-til-psys-fra-infotrygd) for henting av periodedata fra `infotrygd_baq` er avskrudd,
blir testdataene istedenfor krydret med litt tilfeldige uttrekk derfra innimellom, hvor personidentene maskeres.

I motsetning til Bisys som kun er interessert i perioder med utvidet barnetrygd, er Pensjon kun ute etter periodene med ordinær barnetrygd. Derfor har vi filteret
`filter { it.type == YtelseType.ORDINÆR_BARNETRYGD }` i `hentPerioder`funksjonen i ba-sak. I infotrygd gjør vi ikke denne filtreringen pga. sammenslåing av perioder, som betyr at en `UTVIDET_BARNETRYGD`-periode derfra egentlig er en kombinasjon hvor det også inngår ordinære andeler, og må derfor medregnes.

## Nyttige lenker

[Slack-kanal - ba-pensjon-omsorgspoeng](https://nav-it.slack.com/archives/C04JGFS9A3E)

[Swagger - familie-ba-sak (dev)](https://familie-ba-sak.intern.dev.nav.no/swagger-ui/index.html#/pensjon-controller)

[Swagger - familie-ba-infotrygd (dev)](https://familie-ba-infotrygd.intern.dev.nav.no/swagger-ui/index.html#/pensjon-controller)

[Koden som henter barnetrygd fra ba-sak](PensjonService.kt)

[Koden som henter barnetrygd fra infotrygd](https://github.com/navikt/familie-ba-infotrygd/blob/main/src/main/kotlin/no/nav/familie/ba/infotrygd/service/BarnetrygdService.kt#L171)