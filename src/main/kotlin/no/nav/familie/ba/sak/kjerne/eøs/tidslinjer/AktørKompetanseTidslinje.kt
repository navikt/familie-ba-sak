package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import java.time.YearMonth

class AktørKompetanseTidslinje(
    private val aktør: Aktør,
    private val kompetanser: List<Kompetanse>,
) : Tidslinje<Kompetanse, Måned>() {
    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> {
        return kompetanser.map { it.tilPeriode() }
    }

    private fun Kompetanse.tilPeriode() = Periode(
        fraOgMed = this.fom.tilTidspunktEllerUendeligLengeSiden { tom ?: YearMonth.now() },
        tilOgMed = this.tom.tilTidspunktEllerUendeligLengeTil { fom ?: YearMonth.now() },
        innhold = this.copy(fom = null, tom = null, barnAktører = setOf(aktør))
    )
}
