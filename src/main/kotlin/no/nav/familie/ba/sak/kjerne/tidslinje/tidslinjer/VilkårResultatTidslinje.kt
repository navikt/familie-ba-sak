package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_SENESTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

data class VilkårRegelverkResultat(
    val vilkår: Vilkår,
    val regelverk: Regelverk?,
    val resultat: Resultat?
)

class VilkårResultatTidslinje(
    private val vilkårsresultater: List<VilkårResultat>
) : Tidslinje<VilkårRegelverkResultat>() {

    override fun fraOgMed() = vilkårsresultater.minOf {
        it.periodeFom.tilTidspunktEllerUendeligLengeSiden { it.periodeTom!! }.tilInneværendeMåned()
    }

    override fun tilOgMed() = vilkårsresultater.maxOf {
        it.periodeTom.tilTidspunktEllerUendeligLengeTil { it.periodeFom!! }.tilInneværendeMåned()
    }

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat>> {
        return vilkårsresultater.map { it.tilPeriode() }
    }
}

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat> {
    // Forskyv fom til neste måned som blir virkningstidspunktet
    val fom = periodeFom.tilTidspunktEllerUendeligLengeSiden { periodeTom ?: PRAKTISK_SENESTE_DAG }.neste()
    val tom = periodeTom.tilTidspunktEllerUendeligLengeTil { periodeFom!! }
    return Periode(fom, tom, VilkårRegelverkResultat(vilkårType, vurderesEtter, resultat))
}
