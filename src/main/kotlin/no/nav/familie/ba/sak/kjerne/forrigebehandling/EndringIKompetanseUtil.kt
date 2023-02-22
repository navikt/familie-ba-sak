package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned

class EndringIKompetanseUtil {

    fun lagEndringIKompetanseTidslinje(
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>
    ): Tidslinje<Boolean, Måned> {
        val allePersonerMedKompetanser = (nåværendeKompetanser.flatMap { it.barnAktører } + forrigeKompetanser.flatMap { it.barnAktører }).distinct()

        val endringstidslinjerPrPerson = allePersonerMedKompetanser.map { aktør ->
            lagEndringIKompetanseForPersonTidslinje(
                nåværendeKompetanserForPerson = nåværendeKompetanser.filter { it.barnAktører.contains(aktør) },
                forrigeKompetanserForPerson = forrigeKompetanser.filter { it.barnAktører.contains(aktør) }
            )
        }

        return endringstidslinjerPrPerson.kombiner { finnesMinstEnEndringIPeriode(it) }
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>
    ): Boolean = endringer.any { it }

    private fun lagEndringIKompetanseForPersonTidslinje(
        nåværendeKompetanserForPerson: List<Kompetanse>,
        forrigeKompetanserForPerson: List<Kompetanse>
    ): Tidslinje<Boolean, Måned> {
        val nåværendeTidslinje = nåværendeKompetanserForPerson.tilTidslinje()
        val forrigeTidslinje = forrigeKompetanserForPerson.tilTidslinje()

        val endringerTidslinje = nåværendeTidslinje.kombinerUtenNullMed(forrigeTidslinje) { nåværende, forrige ->
            (
                nåværende.søkersAktivitet != forrige.søkersAktivitet ||
                    nåværende.søkersAktivitetsland != forrige.søkersAktivitetsland ||
                    nåværende.annenForeldersAktivitet != forrige.annenForeldersAktivitet ||
                    nåværende.annenForeldersAktivitetsland != forrige.annenForeldersAktivitetsland ||
                    nåværende.barnetsBostedsland != forrige.barnetsBostedsland ||
                    nåværende.resultat != forrige.resultat
                )
        }

        return endringerTidslinje
    }
}
