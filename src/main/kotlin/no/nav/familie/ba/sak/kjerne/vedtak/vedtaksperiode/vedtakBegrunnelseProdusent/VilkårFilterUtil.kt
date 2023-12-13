package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilUtdypendeVilkårsvurderinger
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun ISanityBegrunnelse.erGjeldendeForUtgjørendeVilkår(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    utvidetVilkårPåSøkerIPeriode: VilkårResultatForVedtaksperiode?,
    utvidetVilkårPåSøkerIForrigePeriode: VilkårResultatForVedtaksperiode?,
): Boolean {
    if (this.vilkår.isEmpty()) return false
    val utgjørendeVilkårResultater =
        finnUtgjørendeVilkår(
            begrunnelseGrunnlag = begrunnelseGrunnlag,
            sanityBegrunnelse = this,
            utvidetVilkårPåSøkerIPeriode = utvidetVilkårPåSøkerIPeriode,
            utvidetVilkårPåSøkerIForrigePeriode = utvidetVilkårPåSøkerIForrigePeriode,
        )

    return this.erLikVilkårOgUtdypendeVilkårIPeriode(utgjørendeVilkårResultater)
}

fun ISanityBegrunnelse.erLikVilkårOgUtdypendeVilkårIPeriode(
    vilkårResultaterForPerson: Collection<VilkårResultatForVedtaksperiode>,
): Boolean {
    if (this.vilkår.isEmpty()) return false
    return this.vilkår.all { vilkårISanityBegrunnelse ->
        val vilkårResultat = vilkårResultaterForPerson.find { it.vilkårType == vilkårISanityBegrunnelse }

        vilkårResultat != null && this.matcherMedUtdypendeVilkår(vilkårResultat)
    }
}

fun ISanityBegrunnelse.matcherMedUtdypendeVilkår(vilkårResultat: VilkårResultatForVedtaksperiode): Boolean {
    return when (vilkårResultat.vilkårType) {
        Vilkår.UNDER_18_ÅR -> true
        Vilkår.BOR_MED_SØKER ->
            vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.borMedSokerTriggere) ||
                vilkårResultat.utdypendeVilkårsvurderinger.harMinstEttTriggerFra(this.borMedSokerTriggere)
        Vilkår.GIFT_PARTNERSKAP -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.giftPartnerskapTriggere)
        Vilkår.BOSATT_I_RIKET -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.bosattIRiketTriggere)
        Vilkår.LOVLIG_OPPHOLD -> vilkårResultat.utdypendeVilkårsvurderinger.erLik(this.lovligOppholdTriggere)
        // Håndteres i `erGjeldendeForSmåbarnstillegg`
        Vilkår.UTVIDET_BARNETRYGD -> UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG !in this.utvidetBarnetrygdTriggere
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

private fun Collection<UtdypendeVilkårsvurdering>.harMinstEttTriggerFra(utdypendeVilkårsvurderingFraSanityBegrunnelse: List<VilkårTrigger>): Boolean {
    val utdypendeVilkårPåVilkårResultat = this.toSet()
    val utdypendeVilkårTriggerePåSanityBegrunnelse: Set<UtdypendeVilkårsvurdering> =
        utdypendeVilkårsvurderingFraSanityBegrunnelse.tilUtdypendeVilkårsvurderinger().toSet()

    return utdypendeVilkårPåVilkårResultat.any { utdypendeVilkårTriggerePåSanityBegrunnelse.contains(it) }
}

private fun finnUtgjørendeVilkår(
    sanityBegrunnelse: ISanityBegrunnelse,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    utvidetVilkårPåSøkerIPeriode: VilkårResultatForVedtaksperiode?,
    utvidetVilkårPåSøkerIForrigePeriode: VilkårResultatForVedtaksperiode?,
): Set<VilkårResultatForVedtaksperiode> {
    val vilkårResultater = (begrunnelseGrunnlag.dennePerioden.vilkårResultater + utvidetVilkårPåSøkerIPeriode).filterNotNull()
    val vilkårResultaterForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.vilkårResultater?.plus(
            utvidetVilkårPåSøkerIForrigePeriode,
        )?.filterNotNull()

    val oppfylteVilkårResultaterDennePerioden =
        vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
    val oppfylteVilkårResultaterForrigePeriode =
        vilkårResultaterForrigePeriode?.filter { it.resultat == Resultat.OPPFYLT }
            ?: emptyList()

    val vilkårTjent =
        hentVilkårResultaterTjent(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        )
    val vilkårEndret =
        hentOppfylteVilkårResultaterEndret(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        )
    val vilkårTapt =
        hentVilkårResultaterTapt(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        )

    return if (begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        when (sanityBegrunnelse.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING -> vilkårTjent + vilkårEndret
            SanityPeriodeResultat.INGEN_ENDRING -> vilkårEndret
            SanityPeriodeResultat.IKKE_INNVILGET,
            SanityPeriodeResultat.REDUKSJON,
            -> vilkårTapt + vilkårEndret

            null -> emptyList()
        }
    } else {
        vilkårTapt.takeIf {
            sanityBegrunnelse.periodeResultat in
                listOf(
                    SanityPeriodeResultat.IKKE_INNVILGET,
                    SanityPeriodeResultat.REDUKSJON,
                )
        } ?: emptyList()
    }.toSet()
}

private fun hentOppfylteVilkårResultaterEndret(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<VilkårResultatForVedtaksperiode> =
    oppfylteVilkårResultaterDennePerioden.filter { vilkårResultatForrigePeriode ->
        val sammeVilkårResultatForrigePeriode =
            oppfylteVilkårResultaterForrigePeriode.singleOrNull { it.vilkårType == vilkårResultatForrigePeriode.vilkårType }

        sammeVilkårResultatForrigePeriode != null &&
            vilkårResultatForrigePeriode != sammeVilkårResultatForrigePeriode
    }

private fun hentVilkårResultaterTjent(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<VilkårResultatForVedtaksperiode> {
    val innvilgedeVilkårDennePerioden = oppfylteVilkårResultaterDennePerioden.map { it.vilkårType }
    val innvilgedeVilkårForrigePerioden = oppfylteVilkårResultaterForrigePeriode.map { it.vilkårType }

    val vilkårTjent = innvilgedeVilkårDennePerioden.toSet() - innvilgedeVilkårForrigePerioden.toSet()

    return oppfylteVilkårResultaterDennePerioden.filter { it.vilkårType in vilkårTjent }
}

private fun hentVilkårResultaterTapt(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<VilkårResultatForVedtaksperiode> {
    val oppfyltDennePerioden = oppfylteVilkårResultaterDennePerioden.map { it.vilkårType }.toSet()
    val oppfyltForrigePeriode = oppfylteVilkårResultaterForrigePeriode.map { it.vilkårType }.toSet()

    val vilkårTapt = oppfyltForrigePeriode - oppfyltDennePerioden

    return oppfylteVilkårResultaterForrigePeriode.filter { it.vilkårType in vilkårTapt }
}
