package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

internal class KompetanseTidslinje(
    val kompetanser: List<Kompetanse>
) : Tidslinje<Kompetanse, Måned>() {

    constructor(vararg kompetanser: Kompetanse) : this(kompetanser.toList())

    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> =
        kompetanser.sortedBy { it.fraOgMedTidspunkt() }
            .map { Periode(it.fraOgMedTidspunkt(), it.tilOgMedTidspunkt(), it.utenPeriode()) }
}

private fun Kompetanse.fraOgMedTidspunkt(): Tidspunkt<Måned> =
    this.fom.tilTidspunktEllerTidligereEnn(this.tom)

private fun Kompetanse.tilOgMedTidspunkt(): Tidspunkt<Måned> =
    this.tom.tilTidspunktEllerSenereEnn(this.fom)
