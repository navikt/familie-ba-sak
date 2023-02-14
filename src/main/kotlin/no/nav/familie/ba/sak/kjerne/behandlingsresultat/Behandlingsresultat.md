# Behandlingsresultat
Behandlingsresultatet skal gjenspesile hva som har skjedd i en behandling, og er et resultat av vurderinger og endringer som er gjort i denne behandlingen. Behandlingsresultatet er styrende for hvilken brevmal som skal brukes.

For å utlede behandlingsresultat er det tre ting som peker seg ut som spesielt viktig:
- **Søknad**: Har vi mottatt en søknad eller er det fremstilt krav for noen personer? Isåfall, må vi gi et svar på søknaden i form av innvilgelse/avslag/fortsatt innvilget. 
- **Endringer**: Har noe endret seg siden sist? 
- **Opphør**: Har barnetrygden opphørt i denne behandlingen?

Den tekniske løsningen vi har gått for prøver å utlede de tre aspektene hver for seg, før man til slutt sitter igjen med ett søknadsresultat, ett endringsresultat og ett opphørsresultat som man kan kombinere til et behandlingsresultat.

## 1. Søknadsresultat
Søknadsresultat skal kun genereres for behandlinger med årsak søknad, fødselshendelse eller grunnet manuell migrering. En viktig ting å legge merke til er også at søknadsresultat ikke utledes for _alle_ personer i disse behandlingene, men kun personene det er fremstilt krav for.

Det er ulik utledning for hvilke personer det er fremstilt krav for avhengig av type sak:
- **Søknad**: barn som er krysset av på "Registrer søknad"-steget + søker hvis det er søkt om utvidet
- **Fødselshendelse**: barn som er nye på behandlingen siden forrige gang
- **Manuell migrering**: alle personer i persongrunnlaget

## 2. Endringer
Skal utledes for **alle** behandlinger når det finnes en forrige behandling. Målet med endringsresultatet er å vise om det har vært en endring i behandlingen siden sist. 
Dette kan være både endringer i beløp og endringer i andre ting som ikke påvirker beløpet (som lovverk, kompetanse osv.). 

Mulige resultater:
 - **Endring**: endring i beløp, vilkårsvurdering, kompetanser eller endret utbetaling andeler
 - **Ingen endring**
 
 **OBS! Det er viktig å ikke ta med endringer som også fører til opphørsresultat eller søknadsresultat.** F.eks. det eneste som er gjort på vilkårsvurderingen er å sette sluttdato på et vilkår, noe som fører til opphør. Dette skal ikke utløse resultatet "endring" også.

### Endring i beløp
Hva som regnes som endring i beløp kommer an på om det er fremstilt krav for personen eller ikke.

- For personer fremstilt krav for er det kun en endring når det er fjernet en andel eller en andel er satt til 0 kr. Grunnen til dette er fordi alle andre endringer i beløp blir fanget opp i søknadssteget.
- For alle andre personer: alle endringer i beløp gjelder som endring

### Endring i vilkårsvurdering
Endringer i vilkårsvurdering innebærer:
- Endringer i utdypende vilkårsvurdering
- Endringer i lovverk/regelverk
- Nye splitter i vilkår (utenom å sette tom-dato på vilkår som fører til opphør)

### Endring i kompetanser
På kompetanser regner man endring som endring av:
- Søkers aktivitet
- Søkers aktivitetsland
- Annen forelders aktivitet
- Annen forelders aktivitetsland
- Barnets bostedsland
- Resultat (primærland/sekundærland osv.)

### Endring i endret utbetaling andeler
For endret utbetaling andeler bryr vi oss kun om endringer av:
- Avtaletidspunkt delt bosted
- Årsak
- Søknadstidspunkt

## 3. Opphør
Skal utledes for **alle** behandlinger. Opphørsresultatet reflekterer om det løper barnetrygd (finnes utbetalinger i fremtiden) eller ikke, og om opphøret skjedde i inneværende behandling. 

Mulige resultater:
- **Ikke opphørt**: det løper fortsatt barnetrygd
- **Fortsatt opphørt**: barnetrygden var opphørt forrige behandling og har samme opphørsdato inneværende behandling
- **Opphørt**: 
  - Barnetrygden var ikke opphørt forrige behandling, men er opphørt i denne behandlingen
  - ELLER Barnetrygden var opphørt i forrige behandling, men er opphørt på en tidligere dato i denne behandlingen

## Oppsummering av stegene og deres resultater
| Steg    | Resultater                | Forklaring                                                                                                                                                                                                                                                                                                                                        |
|---------|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Søknad  | Innvilget                 | Flere muligheter (gjelder kun personer fremstilt krav for):<br>1. Det er lagt til en ny andel med beløp > 0<br>2. Det er lagt til en ny andel med beløp satt til 0 kr pga. differanseberegning/delt bosted   <br>3. Andel har endret beløp siden sist, hvor det nye beløpet er større enn 0                                                       |
|         | Avslått                   | Flere muligheter:<br>1. Eksplisitt avslag for person fremstilt krav for<br>2. Lagt til ny andel med beløp satt til 0 kr pga. etterbetaling 3 år/allerede utbetalt/endre mottaker (for person fremstilt krav for) <br> 3. Det finnes uregistrerte barn <br> 4. Fødselshendelse hvor det finnes vilkår som enten er ikke vurdert eller ikke oppfylt |
|         | Delvis innvilget          | Vi har både innvilget og avslått (trenger ikke være på samme person).                                                                                                                                                                                                                                                                             |
|         | Ingen relevante endringer | Ingen av alternativene over. <br>F.eks. hvis en andel er fjernet, eller at andel har samme beløp nå som forrige gang.                                                                                                                                                                                                                             |
 |         | null                      | Ikke søknad/fødselshendelse (dermed ingen personer fremstilt krav for) eller manuell migrering.                                                                                                                                                                                                                                                   |
| Endring | Endringer                 | Flere muligheter:<br>1. Endring i beløp <br>&nbsp; a) For personer fremstilt krav for: kun hvis beløp var større enn 0, men nå er andelen fjernet eller satt til 0kr <br>&nbsp; b) Ellers: alle endringer i beløp <br>2. Endring i vilkårsvurdering<br>3. Endring i endret utbetaling andeler<br>4. Endring i kompetanse                          |
|         | Ingen endringer           | Ingen endring i det som er nevnt i raden over.                                                                                                                                                                                                                                                                                                    |
| Opphør  | Opphørt                   | To muligheter:<br>1. Ikke opphørt i forrige behandling, opphørt nå<br>2. Opphør i forrige behandling, men tidligere opphør i denne behandlingen                                                                                                                                                                                                   |
|         | Fortsatt opphørt          | Forrige behandling var opphørt, og denne behandlingen opphører samme dato som forrige                                                                                                                                                                                                                                                             |
|         | Ikke opphørt              | Ikke opphør i denne behandlingen                                                                                                                                                                                                                                                                                                                  |

## Kombinasjon av resultater
Behandlingsresultat = søknadsresultat + endringsresultat + opphørsresultat

De fleste resultatene forklarer seg selv, som f.eks. "innvilget" + "endring" + "opphørt" = "innvilget, endret og opphørt".

Her kommer et par unntak/rariteter:
- På en søknad/fødselshendelse/manuell migrering må vi gi et resultat på søknaden, og derfor er ikke "Endret og opphørt", "Endret og fortsatt opphørt" eller "Opphørt" gyldige resultater for disse typer saker. I tillegg, "Fortsatt opphørt" er også et ugyldig resultat i disse tilfellene (her må saksbehandler eksplsitt avslå en periode).
- Fortsatt opphørt i kombinasjon med noe annet som er av betydning (f.eks. "Endret") tar ikke med fortsatt opphørt i resultatet. Vi ønsker kun å snakke om det som skjer i _denne_ behandlingen, og kommuniserer derfor kun ut "fortsatt opphørt" om det er det eneste som gjelder.

## Valideringer
- Ikke lov med eksplisitt avslag for personer det ikke er fremstilt krav for
- Søknadsresultat-steget kan ikke returnere null hvis det er søknad/fødselshendelse/manuell migrering