package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull

internal class KompetanseTidslinje(
    val kompetanser: List<Kompetanse>
) : Tidslinje<Kompetanse, Måned>() {

    constructor(vararg kompetanser: Kompetanse) : this(kompetanser.toList())

    override fun fraOgMed() =
        kompetanser.map { it.fraOgMedTidspunkt() }.minsteEllerNull() ?: throw IllegalArgumentException()

    override fun tilOgMed() =
        kompetanser.map { it.tilOgMedTidspunkt() }.størsteEllerNull() ?: throw IllegalArgumentException()

    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> =
        kompetanser.sortedBy { it.fom ?: MIN_MÅNED }
            .map { Periode(it.fraOgMedTidspunkt(), it.tilOgMedTidspunkt(), it.utenPeriode()) }
}

private fun Kompetanse.fraOgMedTidspunkt(): Tidspunkt<Måned> =
    this.fom.tilTidspunktEllerUendeligLengeSiden { this.tom }

private fun Kompetanse.tilOgMedTidspunkt(): Tidspunkt<Måned> =
    this.tom.tilTidspunktEllerUendeligLengeTil { this.fom }
