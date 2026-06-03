package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed

object EndringIEndretUtbetalingAndelUtil {
    fun lagEndringIEndretUbetalingAndelPerPersonTidslinje(
        nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
        forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeEndretAndelerForPerson.tilTidslinje()
        val forrigeTidslinje = forrigeEndretAndelerForPerson.tilTidslinje()

        val endringerTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                val endringIAvtaletidspunktDeltBosted = nåværende?.avtaletidspunktDeltBosted != forrige?.avtaletidspunktDeltBosted
                val endringIÅrsak = nåværende?.årsak != forrige?.årsak && !erKunEndringMellomEtterbetalingsårsaker(nåværende?.årsak, forrige?.årsak)
                val endringISøknadstidspunkt = nåværende?.søknadstidspunkt != forrige?.søknadstidspunkt
                val haddeTidligereIkkeSøknadstidspunkt = forrige?.søknadstidspunkt == null

                endringIAvtaletidspunktDeltBosted ||
                    endringIÅrsak ||
                    (endringISøknadstidspunkt && haddeTidligereIkkeSøknadstidspunkt)
            }

        return endringerTidslinje
    }

    private val etterbetalingsårsaker = setOf(Årsak.ETTERBETALING_3ÅR, Årsak.ETTERBETALING_3MND)

    private fun erKunEndringMellomEtterbetalingsårsaker(
        nåværendeÅrsak: Årsak?,
        forrigeÅrsak: Årsak?,
    ): Boolean = nåværendeÅrsak in etterbetalingsårsaker && forrigeÅrsak in etterbetalingsårsaker
}
