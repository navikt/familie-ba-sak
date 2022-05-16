package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn

class AktørSkjemaTidslinje<S>(
    private val aktør: Aktør,
    private val skjemaer: List<S>,
) : Tidslinje<S, Måned>() where S : PeriodeOgBarnSkjema<S> {
    override fun lagPerioder(): Collection<Periode<S, Måned>> {
        return skjemaer.map { it.tilPeriode() }
    }

    private fun S.tilPeriode() = Periode(
        fraOgMed = this.fom.tilTidspunktEllerTidligereEnn(tom),
        tilOgMed = this.tom.tilTidspunktEllerSenereEnn(fom),
        innhold = this.kopier(fom = null, tom = null, barnAktører = setOf(aktør))
    )
}
