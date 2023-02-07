package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.EndretUtbetalingAndelTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import java.time.YearMonth

internal enum class Opphørsresultat {
    OPPHØRT,
    FORTSATT_OPPHØRT,
    IKKE_OPPHØRT
}

object BehandlingsresultatOpphørUtils {

    internal fun hentOpphørsresultatPåBehandling(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>
    ): Opphørsresultat {
        val nåværendeBehandlingOpphørsdato =
            nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretUtbetalingAndeler = nåværendeEndretAndeler
            )

        val forrigeBehandlingOpphørsdato =
            forrigeAndeler.utledOpphørsdatoForForrigeBehandling(forrigeEndretUtbetalingAndeler = forrigeEndretAndeler)

        val dagensDato = YearMonth.now()

        return when {
            // Rekkefølgen av sjekkene er viktig for å komme fram til riktig opphørsresultat.
            nåværendeBehandlingOpphørsdato == null -> Opphørsresultat.IKKE_OPPHØRT // Både forrige og nåværende behandling har ingen andeler
            nåværendeAndeler.isEmpty() -> Opphørsresultat.OPPHØRT // Alle andeler fra forrige behandling er fjernet
            nåværendeBehandlingOpphørsdato > dagensDato -> Opphørsresultat.IKKE_OPPHØRT
            forrigeBehandlingOpphørsdato > dagensDato || forrigeBehandlingOpphørsdato > nåværendeBehandlingOpphørsdato -> Opphørsresultat.OPPHØRT
            else -> Opphørsresultat.FORTSATT_OPPHØRT
        }
    }

    private fun List<AndelTilkjentYtelse>.finnOpphørsdato() = this.maxOfOrNull { it.stønadTom }?.nesteMåned()

    /**
     * Hvis opphørsdato ikke finnes i denne behandlingen så ønsker vi å bruke tidligste fom-dato fra forrige behandling
     * Ingen opphørsdato i denne behandlingen skjer kun hvis det ikke finnes noen andeler, og da har vi to scenarier:
     * 1. Ingen andeler i denne behandlingen, men andeler i forrige behandling. Da ønsker vi at opphørsdatoen i denne behandlingen skal være "første endring" som altså er lik tidligste fom-dato
     * 2. Ingen andeler i denne behandlingen, ingen andeler i forrige behandling. Da vil denne funksjonen returnere null
     */
    internal fun List<AndelTilkjentYtelse>.utledOpphørsdatoForNåværendeBehandlingMedFallback(
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeEndretUtbetalingAndeler: List<EndretUtbetalingAndel>
    ): YearMonth? {
        return this.filtrerBortIrrelevanteAndeler(endretUtbetalingAndeler = nåværendeEndretUtbetalingAndeler).finnOpphørsdato() ?: forrigeAndeler.minOfOrNull { it.stønadFom }
    }

    /**
     * Hvis det ikke fantes noen andeler i forrige behandling defaulter vi til inneværende måned
     */
    private fun List<AndelTilkjentYtelse>.utledOpphørsdatoForForrigeBehandling(forrigeEndretUtbetalingAndeler: List<EndretUtbetalingAndel>): YearMonth =
        this.filtrerBortIrrelevanteAndeler(endretUtbetalingAndeler = forrigeEndretUtbetalingAndeler).finnOpphørsdato() ?: YearMonth.now().nesteMåned()

    /**
     * Hvis det eksisterer andeler med beløp == 0 så ønsker vi å filtrere bort disse dersom det eksisterer endret utbetaling andel for perioden
     * med årsak ALLEREDE_UTBETALT, ENDRE_MOTTAKER eller ETTERBETALING_3ÅR. Vi grupperer type andeler før vi oppretter tidslinjer da det kan oppstå
     * overlapp hvis vi ikke gjør dette.
     */
    internal fun List<AndelTilkjentYtelse>.filtrerBortIrrelevanteAndeler(endretUtbetalingAndeler: List<EndretUtbetalingAndel>): List<AndelTilkjentYtelse> {
        val personerMedAndeler = this.map { it.aktør }.distinct()

        return personerMedAndeler.flatMap { aktør ->
            val andelerGruppertPerTypePåPerson = this.filter { it.aktør == aktør }.groupBy { it.type }
            val endretUtbetalingAndelerPåPerson = endretUtbetalingAndeler.filter { it.person?.aktør == aktør }

            andelerGruppertPerTypePåPerson.values.flatMap { andelerPerType ->
                filtrerBortIrrelevanteAndelerPerPerson(andelerPerType, endretUtbetalingAndelerPåPerson)
            }
        }
    }

    private fun filtrerBortIrrelevanteAndelerPerPerson(
        andelerPåPersonFiltrertPåType: List<AndelTilkjentYtelse>,
        endretAndelerPåPerson: List<EndretUtbetalingAndel>
    ): List<AndelTilkjentYtelse> {
        val andelTilkjentYtelseTidslinje = AndelTilkjentYtelseTidslinje(andelerPåPersonFiltrertPåType)
        val endretUtbetalingAndelTidslinje = EndretUtbetalingAndelTidslinje(endretAndelerPåPerson)

        return andelTilkjentYtelseTidslinje.kombinerMed(endretUtbetalingAndelTidslinje) { andelTilkjentYtelse, endretUtbetalingAndel ->
            val kalkulertUtbetalingsbeløp = andelTilkjentYtelse?.kalkulertUtbetalingsbeløp ?: 0
            val endringsperiodeÅrsak = endretUtbetalingAndel?.årsak

            when {
                kalkulertUtbetalingsbeløp == 0 && (
                    endringsperiodeÅrsak == Årsak.ALLEREDE_UTBETALT ||
                        endringsperiodeÅrsak == Årsak.ENDRE_MOTTAKER ||
                        endringsperiodeÅrsak == Årsak.ETTERBETALING_3ÅR
                    ) -> null

                else -> andelTilkjentYtelse
            }
        }.tilAndelTilkjentYtelse()
    }
}
