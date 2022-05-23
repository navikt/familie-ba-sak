package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

internal class SkjemaTidslinje<T : PeriodeOgBarnSkjema<T>>(
    val skjemaer: List<T>
) : Tidslinje<T, Måned>() {

    constructor(vararg skjemaer: T) : this(skjemaer.toList())

    override fun lagPerioder(): Collection<Periode<T, Måned>> =
        skjemaer.sortedBy { it.fraOgMedTidspunkt() }
            .map { Periode(it.fraOgMedTidspunkt(), it.tilOgMedTidspunkt(), it.utenPeriode()) }
}

private fun <T : PeriodeOgBarnSkjema<T>> PeriodeOgBarnSkjema<T>.fraOgMedTidspunkt(): Tidspunkt<Måned> =
    this.fom.tilTidspunktEllerTidligereEnn(this.tom)

private fun <T : PeriodeOgBarnSkjema<T>> PeriodeOgBarnSkjema<T>.tilOgMedTidspunkt(): Tidspunkt<Måned> =
    this.tom.tilTidspunktEllerSenereEnn(this.fom)
