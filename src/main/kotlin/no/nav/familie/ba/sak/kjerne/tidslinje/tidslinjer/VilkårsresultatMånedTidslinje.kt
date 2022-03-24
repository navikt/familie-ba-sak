package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

/**
 * Forskyver fom til neste måned som blir i praksis betyr virkningstidspunktet
 */
class VilkårsresultatMånedTidslinje(
    private val vilkårResultatTidslinje: VilkårResultatTidslinje,
) : Tidslinje<VilkårRegelverkResultat, Måned>() {

    override fun fraOgMed() = vilkårResultatTidslinje.fraOgMed().tilInneværendeMåned()

    override fun tilOgMed() = vilkårResultatTidslinje.tilOgMed().tilInneværendeMåned()

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat, Måned>> {
        return vilkårResultatTidslinje.perioder()
            .map { Periode(it.fraOgMed.tilInneværendeMåned().neste(), it.tilOgMed.tilInneværendeMåned(), it.innhold) }
    }
}
