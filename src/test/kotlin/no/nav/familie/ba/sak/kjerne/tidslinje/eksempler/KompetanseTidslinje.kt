package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeTil
import java.time.YearMonth

class KompetanseTidslinje(
    private val kompetanser: List<Kompetanse>,
) : Tidslinje<Kompetanse, YearMonth>() {

    override fun fraOgMed() = kompetanser
        .map { it.fom.tilTidspunktEllerUendeligLengeSiden { it.tom!! } }
        .minOrNull() ?: throw IllegalStateException("Listen av kompetanser er tom")

    override fun tilOgMed() = kompetanser
        .map { it.tom.tilTidspunktEllerUendeligLengeTil { it.fom!! } }
        .maxOrNull() ?: throw IllegalStateException("Listen av kompetanser er tom")

    override fun lagPerioder(): Collection<Periode<Kompetanse, YearMonth>> {
        return kompetanser.map { it.tilPeriode() }
    }
}

fun Kompetanse.tilPeriode() = Periode(
    fraOgMed = this.fom.tilTidspunktEllerUendeligLengeSiden { tom!! },
    tilOgMed = this.tom.tilTidspunktEllerUendeligLengeTil { fom!! },
    innhold = this
)
