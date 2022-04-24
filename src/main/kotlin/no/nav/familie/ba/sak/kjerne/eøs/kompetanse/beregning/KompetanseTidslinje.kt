package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.månedForUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.månedOmUendeligLenge
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull
import java.time.YearMonth

internal class KompetanseTidslinje(
    val kompetanser: List<Kompetanse>
) : Tidslinje<Kompetanse, Måned>() {

    constructor(vararg kompetanser: Kompetanse) : this(kompetanser.toList())

    override fun fraOgMed() =
        kompetanser.map { it.fraOgMedTidspunkt() }.minsteEllerNull() ?: månedForUendeligLengeSiden()

    override fun tilOgMed() =
        kompetanser.map { it.tilOgMedTidspunkt() }.størsteEllerNull() ?: månedOmUendeligLenge()

    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> =
        kompetanser.sortedBy { it.fom ?: MIN_MÅNED }
            .map { Periode(it.fraOgMedTidspunkt(), it.tilOgMedTidspunkt(), it.utenPeriode()) }
}

private fun Kompetanse.fraOgMedTidspunkt(): Tidspunkt<Måned> =
    this.fom.tilTidspunktEllerUendeligLengeSiden { this.tom ?: YearMonth.now() }

private fun Kompetanse.tilOgMedTidspunkt(): Tidspunkt<Måned> = when {
    this.tom != null && this.tom.isAfter(inneværendeMåned()) -> Tidspunkt.uendeligLengeTil(this.tom)
    else -> this.tom.tilTidspunktEllerUendeligLengeTil { this.fom ?: YearMonth.now() }
}
