import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
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
        sanityBegrunnelse = this,
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

private fun finnUtgjørendeVilkår(
    sanityBegrunnelse: ISanityBegrunnelse,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Set<VilkårResultatForVedtaksperiode> {
    val oppfylteVilkårResultaterDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
    val oppfylteVilkårResultaterForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
            ?: emptyList()

    val vilkårTjent = hentVilkårTjent(
        oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
        oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
    )
    val vilkårEndret = hentOppfylteVilkårMedEndretUtdypende(
        oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
        oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
    )
    val vilkårTapt = hentVilkårTapt(
        oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
        oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
    )

    val vilkårResultaterTjent: List<VilkårResultatForVedtaksperiode> =
        oppfylteVilkårResultaterDennePerioden.filter { it.vilkårType in vilkårTjent }
    val vilkårResultaterEndret: List<VilkårResultatForVedtaksperiode> =
        oppfylteVilkårResultaterDennePerioden.filter { it.vilkårType in vilkårEndret }
    val vilkårResultaterTapt: List<VilkårResultatForVedtaksperiode> =
        oppfylteVilkårResultaterForrigePeriode.filter { it.vilkårType in vilkårTapt }

    return if (begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        when (sanityBegrunnelse.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING -> vilkårResultaterTjent + vilkårResultaterEndret
            SanityPeriodeResultat.INGEN_ENDRING -> vilkårResultaterEndret
            SanityPeriodeResultat.IKKE_INNVILGET,
            SanityPeriodeResultat.REDUKSJON,
            -> vilkårResultaterTapt + vilkårResultaterEndret

            null -> emptyList()
        }
    } else {
        vilkårResultaterTapt.takeIf {
            sanityBegrunnelse.periodeResultat in listOf(
                SanityPeriodeResultat.IKKE_INNVILGET,
                SanityPeriodeResultat.REDUKSJON,
            )
        } ?: emptyList()
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
