# Flyt for fødselshendelser

Dette dokumentet beskriver flyten for automatisk behandling av fødselshendelser i familie-ba-sak.

## Oversikt

Når en fødsel blir registrert i folkeregisteret, kan systemet automatisk opprette og behandle en barnetrygdsak. Flyten består av flere steg som validerer at alle vilkår er oppfylt før automatisk vedtak fattes.

## Inngangsport

Fødselshendelser kommer inn i systemet via to hovedveier:

1. **REST API** (`BehandlingController.opprettEllerOppdaterBehandlingFraHendelse`)
   - Mottar en `NyBehandlingHendelse` som inneholder:
     - `morsIdent`: Morens fødselsnummer
     - `barnasIdenter`: Liste over fødselsnummer til barna
   - Oppretter en `BehandleFødselshendelseTask` som settes i kø

2. **Manuell opprettelse** (via interne verktøy)
   - Samme mekanisme som REST API
   - Brukes for testing og feilretting

## Hovedelementer i flyten

### 1. Task-behandling (BehandleFødselshendelseTask)

Når en `BehandleFødselshendelseTask` kjøres:

#### 1.1 Forsinkelse og timing
- Tasken trigges normalt 7 dager etter fødsel
- Hvis opprettet mellom kl 21:00 og 06:00, utsettes kjøring til kl 06:00

#### 1.2 Velg fagsystem (VelgFagSystemService)

Systemet bestemmer om saken skal behandles i BA-sak eller sendes til Infotrygd basert på følgende prioriterte regler:

| Vurdering | Resultat | Beskrivelse |
|-----------|----------|-------------|
| Mor har iverksatte behandlinger i BA-sak | SEND_TIL_BA | Mor har fagsak med tidligere eller løpende utbetalinger |
| Mor eller barn har løpende sak i Infotrygd | SEND_TIL_INFOTRYGD | Løpende sak i Infotrygd funnet |
| Mor har fagsak uten iverksatte behandlinger | SEND_TIL_BA | Fagsak eksisterer, men ingen vedtak |
| Mor har saker i Infotrygd (ikke løpende) | SEND_TIL_INFOTRYGD | Historiske saker i Infotrygd |
| Mor har ikke gyldig statsborgerskap | SEND_TIL_INFOTRYGD | Statsborgerskap ikke støttet for autovedtak |
| Ingen av ovenstående | SEND_TIL_BA | Støttet for automatisk behandling |

**Gyldige statsborgerskap for automatisk vurdering:**
- NORDEN
- EØS
- TREDJELANDSBORGER
- STATSLØS

#### 1.3 Satsendring-sjekk
Før behandling starter sjekkes om mor har gammel sats som må oppdateres. Hvis satsendring er nødvendig:
- Satsendring-behandling opprettes
- Fødselshendelse-behandling utsettes i 60 minutter

### 2. Automatisk behandling (AutovedtakStegService)

Hvis saken skal til BA-sak, starter den automatiske behandlingsflyten.

#### 2.1 Valider om behandling skal kjøres (AutovedtakFødselshendelseService)

**Sjekk om barn allerede behandles:**
- Hvis mor har åpen behandling som inneholder alle barna → avslutt
- Hvis alle barna fra hendelsen er behandlet tidligere → avslutt

**Finn barn som skal behandles:**
- Henter alle barn til mor fra PDL
- Filtrerer bort barn som allerede er behandlet
- Fortsetter kun med nye barn

#### 2.2 Opprett behandling og registrer persongrunnlag
- Oppretter ny behandling av type FØRSTEGANGBEHANDLING med årsak FØDSELSHENDELSE
- Henter personopplysninger fra PDL for mor og barn
- Registrerer persongrunnlag (bostedsadresser, statsborgerskap, sivilstand, etc.)

### 3. Filtreringsregler (FiltreringFødselshendelserSteg)

Filtreringsreglene vurderer om saken egner seg for automatisk behandling. Hvis én regel ikke er oppfylt, henlegges behandlingen automatisk og en manuell oppgave opprettes.

**Hovedregler (evalueringsregler i Filtreringsregel enum):**

| Regel | Beskrivelse | Resultat ved brudd |
|-------|-------------|-------------------|
| MOR_LEVER | Mor må være i live | Henleggelse |
| BARN_LEVER | Alle barn må være i live | Henleggelse |
| MOR_HAR_IKKE_VERGE | Mor kan ikke ha verge | Henleggelse |
| FØDSELSNUMMER_ER_GYLDIG | Barn må ha gyldig fødselsnummer | Henleggelse |
| IKKE_FAGSAKTYPE_INSTITUSJON | Fagsak kan ikke være av type institusjon | Henleggelse |
| LØPENDE_UTVIDET | Spesielle regler for løpende utvidet barnetrygd | Henleggelse hvis ikke oppfylt |
| IKKE_EØS | EØS-saker må behandles manuelt | Henleggelse |
| OPPHØRT_BARNETRYGD | Mor må ikke ha opphørt barnetrygd uten fremtidige andeler | Henleggelse |
| MOR_HAR_IKKE_LØPENDE_UTBETALING_TIL_ANNEN_FORELDER | Barnetrygd kan ikke løpe til annen forelder | Henleggelse |
| MER_ENN_ETT_BARN_PÅ_SAKEN | Regler for flerlinger | Henleggelse hvis ikke oppfylt |
| IKKE_MIGRERT_ETTER_BARN_FØDT | Sak ikke migrert etter barnets fødsel | Henleggelse |

**Fakta som vurderes:**
- Mor: Person-objekt med alle opplysninger
- Mor mottar løpende utvidet barnetrygd
- Mor oppfyller vilkår for utvidet barnetrygd ved fødselsdato
- Mor mottar EØS barnetrygd
- Barn fra hendelse: Liste over barn
- Resten av barna: Mors andre barn
- Dagensdato
- Migreringsinformasjon
- Løpende barnetrygd for annen forelder

**Ved brudd på filtreringsregler:**
- Behandling henlegges med årsak AUTOMATISK_HENLAGT
- Oppgave opprettes for manuell behandling (type: FØDSELSHENDELSE)
- Begrunnelse fra filtreringsregelen legges ved

### 4. Vilkårsvurdering (VilkårsvurderingSteg)

Hvis alle filtreringsregler er oppfylt, vurderes vilkårene for barnetrygd automatisk.

#### 4.1 Vilkår for søker (mor)
- **BOSATT_I_RIKET**: Mor må være bosatt i Norge
- **LOVLIG_OPPHOLD**: Mor må ha lovlig opphold

#### 4.2 Vilkår for barn
- **UNDER_18_ÅR**: Barnet må være under 18 år
- **BOR_MED_SØKER**: Barnet må bo med mor
- **GIFT_PARTNERSKAP**: Barnet kan ikke være gift
- **BOSATT_I_RIKET**: Barnet må være bosatt i Norge
- **LOVLIG_OPPHOLD**: Barnet må ha lovlig opphold

**Automatisk vilkårsvurdering:**
- Vilkårene vurderes basert på data fra PDL (bostedsadresse, sivilstand, etc.)
- Hver vilkårsregel har utfallsårsaker: OPPFYLT, IKKE_OPPFYLT, eller KANSKJE_OPPFYLT
- Hvis alle vilkår er OPPFYLT → behandling fortsetter
- Hvis noen vilkår er IKKE_OPPFYLT eller KANSKJE_OPPFYLT → behandling henlegges

**Ved brudd på vilkår:**
- Behandling henlegges med årsak AUTOMATISK_HENLAGT
- Oppgave opprettes for manuell behandling
- Begrunnelse fra vilkårsvurderingen legges ved (f.eks. "Mor er ikke bosatt i riket")

### 5. Beregning av ytelse

Hvis alle vilkår er oppfylt:
- Beregner tilkjent ytelse basert på satser og perioder
- Genererer vedtaksperioder med begrunnelser
- Begrunnelsene hentes fra `AutovedtakFødselshendelseBegrunnelseService`

### 6. To-trinnskontroll og vedtaksbrev

For godkjente behandlinger:
- To-trinnskontroll opprettes og godkjennes automatisk (beslutter = system)
- Vedtaksbrev genereres
- Status settes til AVSLUTTET

### 7. Iverksetting (IverksettMotOppdragTask)

Siste steg er iverksetting:
- Vedtak sendes til Økonomi (Oppdrag)
- Utbetaling iverksettes
- Distribusjon av vedtaksbrev til bruker

## Feilhåndtering

### Funksjonelle feil
Hvis funksjonell feil oppstår:
- Behandling henlegges ikke automatisk
- Oppgave opprettes: "Vurder livshendelse"
- Saksbehandler må vurdere konsekvens for ytelse manuelt

### Tekniske feil
- Task feiler maksimalt 3 ganger (definert i `@TaskStepBeskrivelse`)
- Etter 3 feil går task til manuell oppfølging

### Spesielle tilfeller
- **Midlertidig enhet**: Hvis mor ikke har norsk adresse, logges melding og behandling stoppes
- **Satsendring pågår**: Task rekjøres om 60 minutter

## Metrics og logging

Systemet logger omfattende metrics for:
- **Fagsystemvalg**: Antall hendelser per utfall (BA-sak vs Infotrygd)
- **Filtreringsregler**: Antall per regel og resultat, inkludert første utfall som stopper
- **Vilkårsvurdering**: Stanset i filtrering vs vilkårsvurdering
- **Gjennomførte**: Antall som passerer alle steg
- **Dager siden barn født**: Fordeling av behandlingstid

Sensitiv informasjon (fødselsnummer, etc.) logges kun til secureLogger.

## Sekvensdiagram

```
Hendelse → BehandleFødselshendelseTask
              ↓
         VelgFagSystemService
              ↓
         BA-sak? ──Nei──→ Send til Infotrygd
              ↓ Ja
         Satsendring?
              ↓ Nei
         AutovedtakFødselshendelseService.skalAutovedtakBehandles
              ↓ Ja
         Opprett behandling + persongrunnlag
              ↓
         FiltreringFødselshendelserSteg
              ↓
         Alle oppfylt? ──Nei──→ Henlegg + Opprett oppgave
              ↓ Ja
         VilkårsvurderingSteg
              ↓
         Alle oppfylt? ──Nei──→ Henlegg + Opprett oppgave
              ↓ Ja
         Beregning + Begrunnelser
              ↓
         To-trinnskontroll + Vedtaksbrev
              ↓
         IverksettMotOppdragTask
              ↓
         Ferdig
```

## Relaterte filer

### Hovedkomponenter
- `BehandleFødselshendelseTask.kt` - Task som starter behandlingen
- `VelgFagSystemService.kt` - Velger mellom BA-sak og Infotrygd
- `AutovedtakFødselshendelseService.kt` - Hovedlogikk for automatisk behandling
- `FiltreringFødselshendelserSteg.kt` - Steg for filtreringsregler
- `FiltreringsreglerService.kt` - Service for evaluering av filtreringsregler

### Vilkårsvurdering
- `vilkårsvurdering/Regler.kt` - Automatiske regler for vilkårsvurdering
- `vilkårsvurdering/utfall/` - Utfallsårsaker for vilkår

### Filtreringsregler
- `filtreringsregler/Filtreringsregel.kt` - Enum med alle filtreringsregler
- `filtreringsregler/FiltreringsreglerFakta.kt` - Fakta brukt i evalueringen

### Støttefunksjoner
- `FlerlingUtils.kt` - Hjelpefunksjoner for flerlinger
- `AutovedtakFødselshendelseBegrunnelseService.kt` - Genererer begrunnelser for vedtak
