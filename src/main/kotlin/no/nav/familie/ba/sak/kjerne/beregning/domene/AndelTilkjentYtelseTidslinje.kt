package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.ekstern.restDomene.RestYtelsePeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil

class RestYtelsePeriodeTidslinje(
    private val restYtelsePeriodeResultat: List<RestYtelsePeriode>,
) : Tidslinje<RestYtelsePeriode, Måned>() {
    override fun lagPerioder(): Collection<Periode<RestYtelsePeriode, Måned>> {
        return restYtelsePeriodeResultat.map { it.tilPeriode() }
    }

    private fun RestYtelsePeriode.tilPeriode() = Periode(
        fraOgMed = this.stønadFom.tilTidspunktEllerUendeligLengeSiden().tilInneværendeMåned(),
        tilOgMed = this.stønadTom.tilTidspunktEllerUendeligLengeTil().tilInneværendeMåned(),
        innhold = this
    )
}
