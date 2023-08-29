import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilUtdypendeVilkårsvurderinger
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun ISanityBegrunnelse.erGjeldendeForUtgjørendeVilkår(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Boolean {
    if (this.vilkår.isEmpty()) return false
    val utgjørendeVilkårResultater = finnUtgjørendeVilkår(
        begrunnelseGrunnlag = begrunnelseGrunnlag,
    )

    return this.erLikVilkårOgUtdypendeVilkårIPeriode(
        utgjørendeVilkårResultater,
    )
}

private fun ISanityBegrunnelse.erLikVilkårOgUtdypendeVilkårIPeriode(
    vilkårResultaterForPerson: Collection<VilkårResultatForVedtaksperiode>,
): Boolean {
    return this.vilkår.all { vilkårISanityBegrunnelse ->
        val vilkårResultat = vilkårResultaterForPerson.find { it.vilkårType == vilkårISanityBegrunnelse }

        vilkårResultat != null && this.matcherMedUtdypendeVilkår(vilkårResultat)
    }
}

fun ISanityBegrunnelse.matcherMedUtdypendeVilkår(vilkårResultat: VilkårResultatForVedtaksperiode): Boolean {
    return when (vilkårResultat.vilkårType) {
        Vilkår.UNDER_18_ÅR -> true
        Vilkår.BOR_MED_SØKER -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.borMedSokerTriggere)
        Vilkår.GIFT_PARTNERSKAP -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.giftPartnerskapTriggere)
        Vilkår.BOSATT_I_RIKET -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.bosattIRiketTriggere)
        Vilkår.LOVLIG_OPPHOLD -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.lovligOppholdTriggere)
        Vilkår.UTVIDET_BARNETRYGD -> true
    }
}

private fun Collection<UtdypendeVilkårsvurdering>.erLik(
    utdypendeVilkårsvurderingFraSanityBegrunnelse: List<VilkårTrigger>?,
): Boolean {
    val utdypendeVilkårPåVilkårResultat = this.toSet()
    val utdypendeVilkårPåSanityBegrunnelse: Set<UtdypendeVilkårsvurdering> =
        utdypendeVilkårsvurderingFraSanityBegrunnelse?.tilUtdypendeVilkårsvurderinger()?.toSet() ?: emptySet()

    return utdypendeVilkårPåVilkårResultat == utdypendeVilkårPåSanityBegrunnelse
}

private fun finnUtgjørendeVilkår(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Set<VilkårResultatForVedtaksperiode> {
    val oppfylteVilkårResultaterDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
    val oppfylteVilkårResultaterForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
            ?: emptyList()

    return if (begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        val vilkårTjentEllerEndretUtdypende = hentVilkårTjent(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        ) + hentOppfylteVilkårMedEndretUtdypende(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        )

        val vilkårTapt = hentVilkårTapt(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        )

        val vilkårResultaterTjentEllerEndret = begrunnelseGrunnlag.dennePerioden.vilkårResultater
            .filter { it.vilkårType in vilkårTjentEllerEndretUtdypende }
        val vilkårResultaterTapt =
            begrunnelseGrunnlag.forrigePeriode?.vilkårResultater?.filter { it.vilkårType in vilkårTapt } ?: emptySet()

        vilkårResultaterTjentEllerEndret + vilkårResultaterTapt
    } else {
        val vilkårTapt = hentVilkårTapt(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        )

        oppfylteVilkårResultaterForrigePeriode.filter { it.vilkårType in vilkårTapt }
    }.toSet()
}

private fun hentOppfylteVilkårMedEndretUtdypende(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<Vilkår> {
    return oppfylteVilkårResultaterForrigePeriode.filter { vilkårResultatForrigePeriode ->
        val sammeVilkårResultatDennePerioden =
            oppfylteVilkårResultaterDennePerioden.singleOrNull { it.vilkårType == vilkårResultatForrigePeriode.vilkårType }
        val utdypendeVilkårsvurderingDennePerioden =
            sammeVilkårResultatDennePerioden?.utdypendeVilkårsvurderinger?.toSet() ?: emptySet()
        val utdypendeVilkårsvurderingForrigePeriode = vilkårResultatForrigePeriode.utdypendeVilkårsvurderinger.toSet()

        utdypendeVilkårsvurderingForrigePeriode != utdypendeVilkårsvurderingDennePerioden
    }.map { it.vilkårType }
}

private fun hentVilkårTjent(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): Set<Vilkår> {
    val innvilgedeVilkårDennePerioden = oppfylteVilkårResultaterDennePerioden.map { it.vilkårType }
    val innvilgedeVilkårForrigePerioden = oppfylteVilkårResultaterForrigePeriode.map { it.vilkårType }

    return (innvilgedeVilkårDennePerioden.toSet() - innvilgedeVilkårForrigePerioden.toSet())
}

private fun hentVilkårTapt(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): Set<Vilkår> {
    val oppfyltDennePerioden = oppfylteVilkårResultaterDennePerioden.map { it.vilkårType }.toSet()

    val oppfyltForrigePeriode = oppfylteVilkårResultaterForrigePeriode.map { it.vilkårType }.toSet()

    return oppfyltForrigePeriode - oppfyltDennePerioden
}
