package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeMedEksterntInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.rangeTo
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil

class KompetanseTidslinje(
    private val kompetanser: List<Kompetanse>,
) : TidslinjeMedEksterntInnhold<Kompetanse>(
    kompetanser
) {

    override fun tidsrom(): Tidsrom {
        val fom = kompetanser.map { it.fom.tilTidspunktEllerUendeligLengeSiden { it.tom!! } }.minOrNull()!!
        val tom = kompetanser.map { it.tom.tilTidspunktEllerUendeligLengeSiden { it.fom!! } }.maxOrNull()!!

        return fom..tom
    }

    override fun perioder(): Collection<Periode<Kompetanse>> {
        return kompetanser.map { it.tilPeriode() }
    }
}

fun Kompetanse.tilPeriode() = Periode(
    fom = this.fom.tilTidspunktEllerUendeligLengeTil { tom!! },
    tom = this.tom.tilTidspunktEllerUendeligLengeTil { fom!! },
    innhold = this
)
