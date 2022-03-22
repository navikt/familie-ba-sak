package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeTil

class KompetanseTidslinje(
    private val kompetanser: List<Kompetanse>,
) : Tidslinje<Kompetanse>() {

    override fun fraOgMed() = kompetanser
        .map { it.fom.tilTidspunktEllerUendeligLengeSiden { it.tom!! } }
        .minOrNull() ?: Tidspunkt.iDag().neste() // Tom liste, sørg for at fraOgMed er etter tilOgMed

    override fun tilOgMed() = kompetanser
        .map { it.tom.tilTidspunktEllerUendeligLengeTil { it.fom!! } }
        .maxOrNull() ?: Tidspunkt.iDag().forrige() // Tom liste, sørg for at tilOgMed er før fraOgMed

    override fun lagPerioder(): Collection<Periode<Kompetanse>> {
        return kompetanser.map { it.tilPeriode() }
    }
}

fun Kompetanse.tilPeriode() = Periode(
    fraOgMed = this.fom.tilTidspunktEllerUendeligLengeSiden { tom!! },
    tilOgMed = this.tom.tilTidspunktEllerUendeligLengeTil { fom!! },
    innhold = this
)
