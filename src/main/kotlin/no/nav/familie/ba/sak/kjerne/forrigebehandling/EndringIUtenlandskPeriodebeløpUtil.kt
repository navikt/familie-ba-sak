package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.tidslinje.Tidslinje

object EndringIUtenlandskPeriodebeløpUtil {
    fun lagEndringIUtenlandskPeriodebeløpForPersonTidslinje(
        nåværendeUtenlandskPeriodebeløpForPerson: List<UtenlandskPeriodebeløp>,
        forrigeUtenlandskPeriodebeløpForPerson: List<UtenlandskPeriodebeløp>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeUtenlandskPeriodebeløpForPerson.tilTidslinje()
        val forrigeTidslinje = forrigeUtenlandskPeriodebeløpForPerson.tilTidslinje()

        val endringerTidslinje =
            nåværendeTidslinje.kombinerUtenNullMed(forrigeTidslinje) { nåværende, forrige ->
                forrige.erObligatoriskeFelterUtenomTidsperioderSatt() && nåværende.felterHarEndretSegSidenForrigeBehandling(forrigeUtenlandskPeriodebeløp = forrige)
            }

        return endringerTidslinje
    }

    private fun UtenlandskPeriodebeløp.felterHarEndretSegSidenForrigeBehandling(forrigeUtenlandskPeriodebeløp: UtenlandskPeriodebeløp): Boolean =
        this.beløp != forrigeUtenlandskPeriodebeløp.beløp ||
            this.intervall != forrigeUtenlandskPeriodebeløp.intervall ||
            this.kalkulertMånedligBeløp != forrigeUtenlandskPeriodebeløp.kalkulertMånedligBeløp ||
            this.valutakode != forrigeUtenlandskPeriodebeløp.valutakode ||
            this.utbetalingsland != forrigeUtenlandskPeriodebeløp.utbetalingsland
}
