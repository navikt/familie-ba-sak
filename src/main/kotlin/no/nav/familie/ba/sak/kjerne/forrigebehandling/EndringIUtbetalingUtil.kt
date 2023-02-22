package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned

class EndringIUtbetalingUtil {

    // Det regnes ikke ut som en endring dersom
    // 1. Vi har fått nye andeler som har 0 i utbetalingsbeløp
    // 2. Vi har mistet andeler som har hatt 0 i utbetalingsbeløp
    // 3. Vi har lik utbetalingsbeløp mellom nåværende og forrige andeler
    fun lagEndringIUtbetalingForPersonOgTypeTidslinje(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>
    ): Tidslinje<Boolean, Måned> {
        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(nåværendeAndeler)
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndeler)

        val endringIBeløpTidslinje = nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
            val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
            val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

            nåværendeBeløp != forrigeBeløp
        }

        return endringIBeløpTidslinje
    }
}
