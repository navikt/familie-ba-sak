package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.BarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

sealed interface ISanityBegrunnelse {
    val apiNavn: String
    val navnISystem: String
    val periodeResultat: SanityPeriodeResultat?
    val vilkår: Set<Vilkår>
    val borMedSokerTriggere: List<VilkårTrigger>
    val giftPartnerskapTriggere: List<VilkårTrigger>
    val bosattIRiketTriggere: List<VilkårTrigger>
    val lovligOppholdTriggere: List<VilkårTrigger>
    val utvidetBarnetrygdTriggere: List<UtvidetBarnetrygdTrigger>
    val fagsakType: FagsakType?
    val tema: Tema?
    val valgbarhet: Valgbarhet?
    val periodeType: BrevPeriodeType?
    val begrunnelseTypeForPerson: VedtakBegrunnelseType? // TODO: Fjern når migrering av ny felter er ferdig

    val gjelderEtterEndretUtbetaling
        get() =
            this is SanityBegrunnelse &&
                this.endretUtbetalingsperiodeTriggere.contains(EndretUtbetalingsperiodeTrigger.ETTER_ENDRET_UTBETALINGSPERIODE)

    val gjelderEndretutbetaling
        get() =
            this is SanityBegrunnelse &&
                this.endringsaarsaker.isNotEmpty() && !gjelderEtterEndretUtbetaling()

    val gjelderSatsendring
        get() =
            this is SanityBegrunnelse &&
                ØvrigTrigger.SATSENDRING in this.ovrigeTriggere
}

data class SanityBegrunnelse(
    override val apiNavn: String,
    override val navnISystem: String,
    override val periodeResultat: SanityPeriodeResultat? = null,
    override val vilkår: Set<Vilkår> = emptySet(),
    override val lovligOppholdTriggere: List<VilkårTrigger> = emptyList(),
    override val bosattIRiketTriggere: List<VilkårTrigger> = emptyList(),
    override val giftPartnerskapTriggere: List<VilkårTrigger> = emptyList(),
    override val borMedSokerTriggere: List<VilkårTrigger> = emptyList(),
    override val utvidetBarnetrygdTriggere: List<UtvidetBarnetrygdTrigger> = emptyList(),
    override val fagsakType: FagsakType? = null,
    override val tema: Tema? = null,
    override val valgbarhet: Valgbarhet? = null,
    override val periodeType: BrevPeriodeType? = null,
    override val begrunnelseTypeForPerson: VedtakBegrunnelseType? = null,
    val rolle: List<VilkårRolle> = emptyList(),
    val ovrigeTriggere: List<ØvrigTrigger> = emptyList(),
    val hjemler: List<String> = emptyList(),
    val hjemlerFolketrygdloven: List<String> = emptyList(),
    val endringsaarsaker: List<Årsak> = emptyList(),
    val endretUtbetalingsperiodeDeltBostedUtbetalingTrigger: EndretUtbetalingsperiodeDeltBostedTriggere? = null,
    val endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger> = emptyList(),
) : ISanityBegrunnelse {
    fun gjelderEtterEndretUtbetaling() =
        this.endretUtbetalingsperiodeTriggere.contains(EndretUtbetalingsperiodeTrigger.ETTER_ENDRET_UTBETALINGSPERIODE)
}

data class SanityEØSBegrunnelse(
    override val apiNavn: String,
    override val navnISystem: String,
    override val periodeResultat: SanityPeriodeResultat? = null,
    override val vilkår: Set<Vilkår>,
    override val borMedSokerTriggere: List<VilkårTrigger> = emptyList(),
    override val fagsakType: FagsakType?,
    override val tema: Tema?,
    override val periodeType: BrevPeriodeType?,
    override val begrunnelseTypeForPerson: VedtakBegrunnelseType? = null,
    override val valgbarhet: Valgbarhet?,
    val annenForeldersAktivitet: List<KompetanseAktivitet>,
    val barnetsBostedsland: List<BarnetsBostedsland>,
    val kompetanseResultat: List<KompetanseResultat>,
    val hjemler: List<String>,
    val hjemlerFolketrygdloven: List<String>,
    val hjemlerEØSForordningen883: List<String>,
    val hjemlerEØSForordningen987: List<String>,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>,
) : ISanityBegrunnelse {
    override val lovligOppholdTriggere: List<VilkårTrigger> = emptyList()
    override val utvidetBarnetrygdTriggere: List<UtvidetBarnetrygdTrigger> = emptyList()
    override val bosattIRiketTriggere: List<VilkårTrigger> = emptyList()
    override val giftPartnerskapTriggere: List<VilkårTrigger> = emptyList()
}
