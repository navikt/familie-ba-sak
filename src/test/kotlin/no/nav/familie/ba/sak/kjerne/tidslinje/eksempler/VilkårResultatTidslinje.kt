package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.rangeTo
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
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
) : TidslinjeUtenAvhengigheter<VilkårRegelverkResultat>() {

    override fun tidsrom(): Tidsrom {
        val fraOgMed: Tidspunkt = vilkårsresultater
            .map { it.periodeFom.tilTidspunktEllerUendeligLengeSiden { it.periodeTom!! }.tilMåned() }.minOrNull()!!
        val tilOgMed: Tidspunkt = vilkårsresultater
            .map { it.periodeTom.tilTidspunktEllerUendeligLengeTil { it.periodeFom!! }.tilMåned() }.maxOrNull()!!

        return fraOgMed..tilOgMed
    }

    override fun perioder(): Collection<Periode<VilkårRegelverkResultat>> {
        return vilkårsresultater.map { it.tilPeriode() }
    }
}

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat> {
    val fom = periodeFom.tilTidspunktEllerUendeligLengeSiden { periodeTom!! }.tilMåned()
    val tom = periodeTom.tilTidspunktEllerUendeligLengeTil { periodeFom!! }.tilMåned()
    return Periode(fom, tom, VilkårRegelverkResultat(vilkårType, vurderesEtter, resultat))
}
