import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVedtakResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilUtdypendeVilkårsvurderinger
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent.BegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.VedtaksperiodeGrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåVilkår(
    begrunnelseGrunnlag: BegrunnelseGrunnlagForPeriode,
    person: Person,
    behandlingUnderkategori: BehandlingUnderkategori,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val vilkårForPerson = Vilkår.hentVilkårFor(
        personType = person.type,
        fagsakType = FagsakType.NORMAL,
        behandlingUnderkategori = behandlingUnderkategori,
    )

    val relevanteBegrunnelser = this.filterValues { it.vilkår.isNotEmpty() }

    val utgjørendeVilkårResultater = finnUtgjørendeVilkår(
        begrunnelseGrunnlag = begrunnelseGrunnlag,
        vilkårForPerson = vilkårForPerson,
    )

    return relevanteBegrunnelser.filtrerBegrunnelserSomMatcherVilkårOgUtdypendeVilkår(
        utgjørendeVilkårResultater,
    )
}

fun erReduksjonDelBostedBegrunnelse(it: SanityBegrunnelse) =
    it.resultat == SanityVedtakResultat.REDUKSJON && it.vilkår.contains(Vilkår.BOR_MED_SØKER) &&
        it.borMedSokerTriggere?.contains(VilkårTrigger.DELT_BOSTED) == true

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerBegrunnelserSomMatcherVilkårType(
    vilkårForPerson: Collection<Vilkår>,
) = this.filterValues { sanityBegrunnelse -> sanityBegrunnelse.vilkår.all { it in vilkårForPerson } }

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerBegrunnelserSomMatcherVilkårOgUtdypendeVilkår(
    vilkårResultaterForPerson: Collection<VilkårResultatForVedtaksperiode>,
) = this.filterValues { sanityBegrunnelse ->
    sanityBegrunnelse.vilkår.all { vilkårISanityBegrunnelse ->
        val vilkårResultat = vilkårResultaterForPerson.find { it.vilkårType == vilkårISanityBegrunnelse }

        vilkårResultat != null && sanityBegrunnelse.matcherMedUtdypendeVilkår(vilkårResultat)
    }
}

fun SanityBegrunnelse.matcherMedUtdypendeVilkår(vilkårResultat: VilkårResultatForVedtaksperiode): Boolean {
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
    begrunnelseGrunnlag: BegrunnelseGrunnlagForPeriode,
    vilkårForPerson: Set<Vilkår>,
): Set<VilkårResultatForVedtaksperiode> {
    val oppfylteVilkårResultaterDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
    val oppfylteVilkårResultaterForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
            ?: emptyList()

    return if (begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        val vilkårTjentEllerEndrerUtbetaling = hentVilkårTjent(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        ) + hentVilkårSomFørerTilØkingEllerReduksjonAvUtbetaling(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
        )

        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
            .filter { it.vilkårType in vilkårTjentEllerEndrerUtbetaling }
    } else {
        val vilkårTapt = hentVilkårTapt(
            oppfylteVilkårResultaterDennePerioden = oppfylteVilkårResultaterDennePerioden,
            oppfylteVilkårResultaterForrigePeriode = oppfylteVilkårResultaterForrigePeriode,
            vilkårForPerson,
        )

        oppfylteVilkårResultaterForrigePeriode.filter { it.vilkårType in vilkårTapt }
    }.toSet()
}

private fun VedtaksperiodeGrunnlagForPerson.hentOppfylteVilkår() =
    vilkårResultaterForVedtaksperiode.filter { it.resultat == Resultat.OPPFYLT }

private fun hentVilkårSomFørerTilØkingEllerReduksjonAvUtbetaling(
    oppfylteVilkårResultaterDennePerioden: List<VilkårResultatForVedtaksperiode>,
    oppfylteVilkårResultaterForrigePeriode: List<VilkårResultatForVedtaksperiode>,
): List<Vilkår> {
    val oppfyltBorMedSøkerDennePerioden =
        oppfylteVilkårResultaterDennePerioden.singleOrNull { it.vilkårType == Vilkår.BOR_MED_SØKER && it.resultat == Resultat.OPPFYLT }
    val oppfyltBorMedSøkerForrigePeriode =
        oppfylteVilkårResultaterForrigePeriode.singleOrNull { it.vilkårType == Vilkår.BOR_MED_SØKER && it.resultat == Resultat.OPPFYLT }

    return when {
        oppfyltBorMedSøkerDennePerioden == null -> emptyList()
        oppfyltBorMedSøkerForrigePeriode == null -> emptyList()

        // Barnetrygden reduseres fra full til delt
        !oppfyltBorMedSøkerDennePerioden.erDeltBosted() && oppfyltBorMedSøkerForrigePeriode.erDeltBosted() ->
            listOf(Vilkår.BOR_MED_SØKER)

        // Barnetrygden øker fra delt til full
        oppfyltBorMedSøkerDennePerioden.erDeltBosted() && !oppfyltBorMedSøkerForrigePeriode.erDeltBosted() ->
            listOf(Vilkår.BOR_MED_SØKER)

        else -> emptyList()
    }
}

private fun VilkårResultatForVedtaksperiode.erDeltBosted() =
    UtdypendeVilkårsvurdering.DELT_BOSTED in this.utdypendeVilkårsvurderinger

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
    vilkårForPerson: Set<Vilkår>,
): Set<Vilkår> {
    val manglendeVilkårDennePerioden =
        vilkårForPerson - oppfylteVilkårResultaterDennePerioden.map { it.vilkårType }.toSet()

    val manglendeVilkårForrigePerioden =
        vilkårForPerson - oppfylteVilkårResultaterForrigePeriode.map { it.vilkårType }.toSet()

    return manglendeVilkårDennePerioden - manglendeVilkårForrigePerioden
}
