package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.stemmerMedVilkårsvurdering
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
    return this.vilkår.any { vilkårISanityBegrunnelse ->
        val vilkårResultat = vilkårResultaterForPerson.find { it.vilkårType == vilkårISanityBegrunnelse }

        vilkårResultat != null && this.matcherMedUtdypendeVilkår(vilkårResultat)
    }
}

fun ISanityBegrunnelse.matcherMedUtdypendeVilkår(vilkårResultat: VilkårResultatForVedtaksperiode): Boolean {
    return when (vilkårResultat.vilkårType) {
        Vilkår.UNDER_18_ÅR -> true
        Vilkår.BOR_MED_SØKER -> vilkårResultat.utdypendeVilkårsvurderinger.harMinstEnTriggerFra(this.borMedSokerTriggere)
        Vilkår.GIFT_PARTNERSKAP -> vilkårResultat.utdypendeVilkårsvurderinger.harMinstEnTriggerFra(this.giftPartnerskapTriggere)
        Vilkår.BOSATT_I_RIKET -> vilkårResultat.utdypendeVilkårsvurderinger.harMinstEnTriggerFra(this.bosattIRiketTriggere)
        Vilkår.LOVLIG_OPPHOLD -> vilkårResultat.utdypendeVilkårsvurderinger.harMinstEnTriggerFra(this.lovligOppholdTriggere)
        // Håndteres i `erGjeldendeForSmåbarnstillegg`
        Vilkår.UTVIDET_BARNETRYGD -> UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG !in this.utvidetBarnetrygdTriggere
    }
}

private fun List<UtdypendeVilkårsvurdering>.harMinstEnTriggerFra(utdypendeVilkårsvurderingFraSanityBegrunnelse: List<VilkårTrigger>): Boolean {
    return utdypendeVilkårsvurderingFraSanityBegrunnelse.isEmpty() ||
        utdypendeVilkårsvurderingFraSanityBegrunnelse.any {
            it.stemmerMedVilkårsvurdering(utdypendeVilkårPåVilkårResultat = this)
        }
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

    val erOrdinæreVilkårInnvilgetForrigePeriodeEllerForrigeBehandling =
        begrunnelseGrunnlag.forrigePeriode?.erOrdinæreVilkårInnvilget() ?: false ||
            begrunnelseGrunnlag.sammePeriodeForrigeBehandling?.erOrdinæreVilkårInnvilget() ?: false

    return if (begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        when (sanityBegrunnelse.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING -> vilkårTjent + vilkårEndret
            SanityPeriodeResultat.INGEN_ENDRING -> vilkårEndret
            SanityPeriodeResultat.IKKE_INNVILGET,
            SanityPeriodeResultat.REDUKSJON,
            -> vilkårTapt + vilkårEndret

            SanityPeriodeResultat.IKKE_RELEVANT,
            null,
            -> emptyList()
        }
    } else {
        vilkårTapt.takeIf {
            erOrdinæreVilkårInnvilgetForrigePeriodeEllerForrigeBehandling &&
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
