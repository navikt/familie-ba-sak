package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.lagOgValiderPeriodeFraVilkår
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

data class MinimertVilkårResultat(
    val vilkårType: Vilkår,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val erEksplisittAvslagPåSøknad: Boolean?,
) {

    fun toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(
        this.periodeFom,
        this.periodeTom,
        this.erEksplisittAvslagPåSøknad
    )
}

fun VilkårResultat.tilMinimertVilkårResultat() =
    MinimertVilkårResultat(
        vilkårType = this.vilkårType,
        periodeFom = this.periodeFom,
        periodeTom = this.periodeTom,
        resultat = this.resultat,
        utdypendeVilkårsvurderinger = this.utdypendeVilkårsvurderinger,
        erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
    )

