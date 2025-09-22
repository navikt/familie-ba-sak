package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger

fun ISanityBegrunnelse.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    if (begrunnelseGrunnlag !is BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger) {
        return false
    }

    val oppfylteVilkårDenneBehandlingen =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater
            .filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkårType }
            .toSet()
    val oppfylteVilkårForrigeBehandling =
        begrunnelseGrunnlag.sammePeriodeForrigeBehandling
            ?.vilkårResultater
            ?.filter { it.resultat == Resultat.OPPFYLT }
            ?.map { it.vilkårType }
            ?.toSet() ?: emptySet()

    val vilkårMistetSidenForrigeBehandling = oppfylteVilkårForrigeBehandling - oppfylteVilkårDenneBehandlingen

    val begrunnelseGjelderMistedeVilkår = this.vilkår.all { it in vilkårMistetSidenForrigeBehandling }

    val begrunnelseGjelderTaptSmåbarnstillegg = sjekkOmBegrunnelseGjelderTaptSmåbarnstillegg(begrunnelseGrunnlag)
    val begrunnelseGjelderTaptFinnmarkstillegg = sjekkOmBegrunnelseGjelderTaptFinnmarkstillegg(begrunnelseGrunnlag)
    val begrunnelseGjelderTaptSvalbardtillegg = sjekkOmBegrunnelseGjelderTaptSvalbardtillegg(begrunnelseGrunnlag)

    return begrunnelseSkalTriggesForReduksjonFraForrigeBehandling() &&
        (
            begrunnelseGjelderMistedeVilkår ||
                begrunnelseGjelderTaptSmåbarnstillegg ||
                begrunnelseGjelderTaptFinnmarkstillegg ||
                begrunnelseGjelderTaptSvalbardtillegg
        )
}

private fun ISanityBegrunnelse.sjekkOmBegrunnelseGjelderTaptSmåbarnstillegg(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    val haddeSmåbarnstilleggForrigeBehandling = begrunnelseGrunnlag.erSmåbarnstilleggIForrigeBehandlingPeriode()
    val harSmåbarnstilleggDennePerioden = begrunnelseGrunnlag.dennePerioden.andeler.any { it.type == YtelseType.SMÅBARNSTILLEGG }
    val begrunnelseGjelderTaptSmåbarnstillegg = UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG in utvidetBarnetrygdTriggere && haddeSmåbarnstilleggForrigeBehandling && !harSmåbarnstilleggDennePerioden
    return begrunnelseGjelderTaptSmåbarnstillegg
}

private fun ISanityBegrunnelse.sjekkOmBegrunnelseGjelderTaptFinnmarkstillegg(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    val haddeKravPåFinnmarkstilleggForrigeBehandling = begrunnelseGrunnlag.sjekkOmharKravPåFinnmarkstilleggIForrigeBehandlingPeriode()
    val harKravPåFinnmarkstilleggDennePerioden = begrunnelseGrunnlag.sjekkOmHarKravPåFinnmarkstilleggDennePeriode()
    val begrunnelseGjelderTaptFinnmarkstillegg = VilkårTrigger.BOSATT_I_FINNMARK_NORD_TROMS in bosattIRiketTriggere && haddeKravPåFinnmarkstilleggForrigeBehandling && !harKravPåFinnmarkstilleggDennePerioden
    return begrunnelseGjelderTaptFinnmarkstillegg
}

private fun ISanityBegrunnelse.sjekkOmBegrunnelseGjelderTaptSvalbardtillegg(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    val haddeKravPåSvalbardtilleggForrigeBehandling = begrunnelseGrunnlag.sjekkOmharKravPåSvalbardtilleggIForrigeBehandlingPeriode()
    val harKravPåSvalbardtilleggDennePerioden = begrunnelseGrunnlag.sjekkOmHarKravPåSvalbardtilleggDennePeriode()
    val begrunnelseGjelderTaptSvalbardtillegg = VilkårTrigger.BOSATT_PÅ_SVALBARD in bosattIRiketTriggere && haddeKravPåSvalbardtilleggForrigeBehandling && !harKravPåSvalbardtilleggDennePerioden
    return begrunnelseGjelderTaptSvalbardtillegg
}

internal fun ISanityBegrunnelse.begrunnelseSkalTriggesForReduksjonFraForrigeBehandling() = ØvrigTrigger.REDUKSJON_FRA_FORRIGE_BEHANDLING in this.øvrigeTriggere
