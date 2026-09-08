package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

object EndringIUtbetalingUtil {
    fun utledEndringstidspunktForUtbetalingsbeløp(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): YearMonth? {
        val endringIUtbetalingTidslinje =
            lagEndringIUtbetalingTidslinje(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
            )

        return endringIUtbetalingTidslinje.tilFørsteEndringstidspunkt()
    }

    internal fun lagEndringIUtbetalingTidslinje(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean> {
        val allePersonerMedAndeler = (nåværendeAndeler.map { it.aktør } + forrigeAndeler.map { it.aktør }).distinct()

        val endringstidslinjePerPersonOgType =
            allePersonerMedAndeler.flatMap { aktør ->
                val ytelseTyperForPerson = (nåværendeAndeler.map { it.type } + forrigeAndeler.map { it.type }).distinct()

                ytelseTyperForPerson.map { ytelseType ->
                    lagEndringIUtbetalingForPersonOgTypeTidslinje(
                        nåværendeAndeler = nåværendeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                        forrigeAndeler = forrigeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    )
                }
            }

        return endringstidslinjePerPersonOgType.kombiner { finnesMinstEnEndringIPeriode(it) }
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>,
    ): Boolean = endringer.any { it }

    // Det regnes ikke ut som en endring dersom
    // 1. Vi har fått nye andeler som har 0 i utbetalingsbeløp
    // 2. Vi har mistet andeler som har hatt 0 i utbetalingsbeløp
    // 3. Vi har lik utbetalingsbeløp mellom nåværende og forrige andeler
    private fun lagEndringIUtbetalingForPersonOgTypeTidslinje(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeAndeler.tilTidslinje()
        val forrigeTidslinje = forrigeAndeler.tilTidslinje()

        val endringIBeløpTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
                val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

                nåværendeBeløp != forrigeBeløp
            }

        return endringIBeløpTidslinje
    }

    internal fun lagEtterbetalingstidslinjeForPersonOgType(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeAndeler.tilTidslinje()
        val forrigeTidslinje = forrigeAndeler.tilTidslinje()

        val etterbetaling =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
                val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

                nåværendeBeløp > forrigeBeløp
            }

        return etterbetaling
    }

    internal fun finnAktørerMedEndringIAndeler(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Set<Aktør> {
        val nåværendeAndelerPerAktørOgType = nåværendeAndeler.groupBy { it.aktør to it.type }
        val forrigeAndelerPerAktørOgType = forrigeAndeler.groupBy { it.aktør to it.type }

        return (nåværendeAndelerPerAktørOgType.keys + forrigeAndelerPerAktørOgType.keys)
            .filter { aktørOgType ->
                lagEndringIAndelForPersonOgTypeTidslinje(
                    nåværendeAndeler = nåværendeAndelerPerAktørOgType[aktørOgType].orEmpty(),
                    forrigeAndeler = forrigeAndelerPerAktørOgType[aktørOgType].orEmpty(),
                ).tilPerioder().any { it.verdi == true }
            }.map { (aktør, _) -> aktør }
            .toSet()
    }

    private fun lagEndringIAndelForPersonOgTypeTidslinje(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeAndeler.tilTidslinje()
        val forrigeTidslinje = forrigeAndeler.tilTidslinje()

        return nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
            when {
                nåværende == null && forrige == null -> {
                    false
                }

                nåværende == null || forrige == null -> {
                    true
                }

                else -> {
                    nåværende.sats != forrige.sats ||
                        nåværende.prosent != forrige.prosent ||
                        nåværende.kalkulertUtbetalingsbeløp != forrige.kalkulertUtbetalingsbeløp ||
                        nåværende.nasjonaltPeriodebeløp != forrige.nasjonaltPeriodebeløp ||
                        nåværende.differanseberegnetPeriodebeløp != forrige.differanseberegnetPeriodebeløp ||
                        nåværende.beløpUtenEndretUtbetaling != forrige.beløpUtenEndretUtbetaling
                }
            }
        }
    }
}
