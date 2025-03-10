package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNullMed
import no.nav.familie.tidslinje.Tidslinje

object EndringIKompetanseUtil {
    fun lagEndringIKompetanseForPersonTidslinje(
        nåværendeKompetanserForPerson: List<Kompetanse>,
        forrigeKompetanserForPerson: List<Kompetanse>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeKompetanserForPerson.tilTidslinje()
        val forrigeTidslinje = forrigeKompetanserForPerson.tilTidslinje()

        val endringerTidslinje =
            nåværendeTidslinje.kombinerUtenNullMed(forrigeTidslinje) { nåværende, forrige ->
                forrige.erObligatoriskeFelterUtenomTidsperioderSatt() && nåværende.felterHarEndretSegSidenForrigeBehandling(forrigeKompetanse = forrige)
            }

        return endringerTidslinje
    }

    private fun Kompetanse.felterHarEndretSegSidenForrigeBehandling(forrigeKompetanse: Kompetanse): Boolean =
        this.søkersAktivitet != forrigeKompetanse.søkersAktivitet ||
            this.søkersAktivitetsland != forrigeKompetanse.søkersAktivitetsland ||
            this.annenForeldersAktivitet != forrigeKompetanse.annenForeldersAktivitet ||
            this.annenForeldersAktivitetsland != forrigeKompetanse.annenForeldersAktivitetsland ||
            this.barnetsBostedsland != forrigeKompetanse.barnetsBostedsland ||
            this.resultat != forrigeKompetanse.resultat
}
