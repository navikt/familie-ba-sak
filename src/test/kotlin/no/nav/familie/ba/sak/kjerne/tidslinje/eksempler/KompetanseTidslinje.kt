package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidsrom
import no.nav.familie.ba.sak.kjerne.tidslinje.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidspunktEllerUendeligLengeTil

class KompetanseTidslinje(
    private val kompetanser: List<Kompetanse>,
) : Tidslinje<Kompetanse>() {

    override fun tidsrom(): Tidsrom {
        val fom = kompetanser.map { it.fom.tilTidspunktEllerUendeligLengeSiden { it.tom!! } }.minOrNull()!!
        val tom = kompetanser.map { it.tom.tilTidspunktEllerUendeligLengeTil { it.fom!! } }.maxOrNull()!!

        return fom..tom
    }

    override fun perioder(): Collection<Periode<Kompetanse>> {
        return kompetanser.map { it.tilPeriode() }
    }
}

fun Kompetanse.tilPeriode() = Periode(
    fom = this.fom.tilTidspunktEllerUendeligLengeSiden { tom!! },
    tom = this.tom.tilTidspunktEllerUendeligLengeTil { fom!! },
    innhold = this
)
