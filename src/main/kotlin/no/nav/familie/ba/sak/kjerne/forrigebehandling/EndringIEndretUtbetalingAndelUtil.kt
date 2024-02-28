package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.beregning.EndretUtbetalingAndelTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned

object EndringIEndretUtbetalingAndelUtil {
    fun lagEndringIEndretUbetalingAndelPerPersonTidslinje(
        nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
        forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
    ): Tidslinje<Boolean, Måned> {
        val nåværendeTidslinje = EndretUtbetalingAndelTidslinje(nåværendeEndretAndelerForPerson)
        val forrigeTidslinje = EndretUtbetalingAndelTidslinje(forrigeEndretAndelerForPerson)

        val endringerTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                (
                    nåværende?.avtaletidspunktDeltBosted != forrige?.avtaletidspunktDeltBosted ||
                        nåværende?.årsak != forrige?.årsak ||
                        nåværende?.søknadstidspunkt != forrige?.søknadstidspunkt
                )
            }

        return endringerTidslinje
    }
}
