package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.YearMonth

data class VilkårRegelverkResultat(
    val vilkår: Vilkår,
    val regelverk: Regelverk?,
    val resultat: Resultat?
)

class VilkårResultatTidslinje(
    private val vilkårsresultater: List<VilkårResultat>
) : Tidslinje<VilkårRegelverkResultat, YearMonth>() {

    override fun fraOgMed() = vilkårsresultater
        .map { it.periodeFom.tilTidspunktEllerUendeligLengeSiden { it.periodeTom!! }.tilInneværendeMåned() }
        .minOrNull() ?: throw IllegalStateException("Mangler vilkårsresultater")

    override fun tilOgMed() = vilkårsresultater
        .map { it.periodeTom.tilTidspunktEllerUendeligLengeTil { it.periodeFom!! }.tilInneværendeMåned() }
        .maxOrNull() ?: throw IllegalStateException("Mangler vilkårsresultater")

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat, YearMonth>> {
        return vilkårsresultater.map { it.tilPeriode() }
    }
}

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat, YearMonth> {
    val fom = periodeFom.tilTidspunktEllerUendeligLengeSiden { periodeTom!! }.tilInneværendeMåned()
    val tom = periodeTom.tilTidspunktEllerUendeligLengeTil { periodeFom!! }.tilInneværendeMåned()
    return Periode(fom, tom, VilkårRegelverkResultat(vilkårType, vurderesEtter, resultat))
}
