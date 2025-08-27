package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
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
                val endringIÅrsak = nåværende?.årsak != forrige?.årsak
                val endringISøknadstidspunkt = nåværende?.søknadstidspunkt != forrige?.søknadstidspunkt
                val haddeTidligereIkkeSøknadstidspunkt = forrige?.søknadstidspunkt == null

                endringIAvtaletidspunktDeltBosted ||
                    endringIÅrsak ||
                    (endringISøknadstidspunkt && haddeTidligereIkkeSøknadstidspunkt)
            }

        return endringerTidslinje
    }
}
