package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.førerTilOpphør
import java.time.YearMonth

internal enum class Opphørsresultat {
    OPPHØRT,
    FORTSATT_OPPHØRT,
    IKKE_OPPHØRT,
}

object BehandlingsresultatOpphørUtils {
    internal fun hentOpphørsresultatPåBehandling(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
    ): Opphørsresultat {
        val nåværendeBehandlingOpphørsdato =
            nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                nåværendeEndretAndelerIBehandling = nåværendeEndretAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
            )

        val forrigeBehandlingOpphørsdato =
            forrigeAndeler.utledOpphørsdatoForForrigeBehandling(forrigeEndretAndeler = forrigeEndretAndeler)

        val nesteMåned = YearMonth.now().plusMonths(1)

        return when {
            // Rekkefølgen av sjekkene er viktig for å komme fram til riktig opphørsresultat.
            nåværendeBehandlingOpphørsdato == null -> Opphørsresultat.IKKE_OPPHØRT

            // Både forrige og nåværende behandling har ingen andeler
            nåværendeBehandlingOpphørsdato <= nesteMåned && forrigeBehandlingOpphørsdato > nåværendeBehandlingOpphørsdato -> Opphørsresultat.OPPHØRT

            // Nåværende behandling er opphørt og forrige har senere opphørsdato
            nåværendeBehandlingOpphørsdato <= nesteMåned && nåværendeBehandlingOpphørsdato == forrigeBehandlingOpphørsdato -> Opphørsresultat.FORTSATT_OPPHØRT

            else -> Opphørsresultat.IKKE_OPPHØRT
        }
    }

    private fun List<AndelTilkjentYtelse>.finnOpphørsdato() = this.maxOfOrNull { it.stønadTom }?.nesteMåned()

    /**
     * Hvis opphørsdato ikke finnes i denne behandlingen så ønsker vi å bruke tidligste fom-dato fra forrige behandling
     * Ingen opphørsdato i denne behandlingen skjer kun hvis det ikke finnes noen andeler, og da har vi to scenarier:
     * 1. Ingen andeler i denne behandlingen, men andeler i forrige behandling. Da ønsker vi at opphørsdatoen i denne behandlingen skal være "første endring" som altså er lik tidligste fom-dato
     * 2. Ingen andeler i denne behandlingen, ingen andeler i forrige behandling. Da vil denne funksjonen returnere null
     */
    fun List<AndelTilkjentYtelse>.utledOpphørsdatoForNåværendeBehandlingMedFallback(
        forrigeAndelerIBehandling: List<AndelTilkjentYtelse>,
        nåværendeEndretAndelerIBehandling: List<EndretUtbetalingAndel>,
        endretAndelerForForrigeBehandling: List<EndretUtbetalingAndel>,
    ): YearMonth? =
        this.filtrerBortIrrelevanteAndeler(endretAndeler = nåværendeEndretAndelerIBehandling).finnOpphørsdato()
            ?: forrigeAndelerIBehandling.filtrerBortIrrelevanteAndeler(endretAndeler = endretAndelerForForrigeBehandling).minOfOrNull { it.stønadFom }

    /**
     * Hvis det ikke fantes noen andeler i forrige behandling defaulter vi til inneværende måned
     */
    private fun List<AndelTilkjentYtelse>.utledOpphørsdatoForForrigeBehandling(forrigeEndretAndeler: List<EndretUtbetalingAndel>): YearMonth =
        this.filtrerBortIrrelevanteAndeler(endretAndeler = forrigeEndretAndeler).finnOpphørsdato() ?: YearMonth
            .now()
            .nesteMåned()

    /**
     * Hvis det eksisterer andeler med beløp == 0 så ønsker vi å filtrere bort disse dersom det eksisterer endret utbetaling andel for perioden
     * med årsak ALLEREDE_UTBETALT, ENDRE_MOTTAKER eller ETTERBETALING_3ÅR. Vi grupperer type andeler før vi oppretter tidslinjer da det kan oppstå
     * overlapp hvis vi ikke gjør dette.
     */
    internal fun List<AndelTilkjentYtelse>.filtrerBortIrrelevanteAndeler(endretAndeler: List<EndretUtbetalingAndel>): List<AndelTilkjentYtelse> {
        val personerMedAndeler = this.map { it.aktør }.distinct()

        return personerMedAndeler.flatMap { aktør ->
            val andelerGruppertPerTypePåPerson = this.filter { it.aktør == aktør }.groupBy { it.type }
            val endretUtbetalingAndelerPåPerson = endretAndeler.filter { it.personer.any { person -> person.aktør == aktør } }

            andelerGruppertPerTypePåPerson.values.flatMap { andelerPerType ->
                filtrerBortIrrelevanteAndelerPerPersonOgType(andelerPerType, endretUtbetalingAndelerPåPerson)
            }
        }
    }

    private fun filtrerBortIrrelevanteAndelerPerPersonOgType(
        andelerPåPersonFiltrertPåType: List<AndelTilkjentYtelse>,
        endretAndelerPåPerson: List<EndretUtbetalingAndel>,
    ): List<AndelTilkjentYtelse> {
        val andelerMedEndringer =
            andelerPåPersonFiltrertPåType.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endretAndelerPåPerson)

        val andelerMedRelevantUtbetaling =
            andelerMedEndringer.filterNot { andelTilkjentYtelseMedEndreteUtbetalinger ->
                andelTilkjentYtelseMedEndreteUtbetalinger.endreteUtbetalinger.any { it.førerTilOpphør() }
            }

        return andelerMedRelevantUtbetaling.map { it.andel }
    }
}
