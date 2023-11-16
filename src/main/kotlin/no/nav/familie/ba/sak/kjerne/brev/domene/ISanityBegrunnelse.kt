package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.BarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
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
    val ovrigeTriggere: List<ØvrigTrigger>

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
    override val ovrigeTriggere: List<ØvrigTrigger> = emptyList(),
    @Deprecated("Bruk vilkår")
    val vilkaar: List<SanityVilkår> = emptyList(),
    val rolle: List<VilkårRolle> = emptyList(),
    val hjemler: List<String> = emptyList(),
    val hjemlerFolketrygdloven: List<String> = emptyList(),
    val endringsaarsaker: List<Årsak> = emptyList(),
    val endretUtbetalingsperiodeDeltBostedUtbetalingTrigger: EndretUtbetalingsperiodeDeltBostedTriggere? = null,
    val endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger> = emptyList(),
) : ISanityBegrunnelse {
    val triggesAv: TriggesAv by lazy { this.tilTriggesAv() }

    fun gjelderEtterEndretUtbetaling() =
        this.endretUtbetalingsperiodeTriggere.contains(EndretUtbetalingsperiodeTrigger.ETTER_ENDRET_UTBETALINGSPERIODE)
}

enum class ØvrigTrigger {
    MANGLER_OPPLYSNINGER,
    SATSENDRING,
    BARN_MED_6_ÅRS_DAG,
    ALLTID_AUTOMATISK,
    ETTER_ENDRET_UTBETALING,
    ENDRET_UTBETALING,
    OPPHØR_FRA_FORRIGE_BEHANDLING,
    REDUKSJON_FRA_FORRIGE_BEHANDLING,
    BARN_DØD,
    SKAL_VISES_SELV_OM_IKKE_ENDRING,

    @Deprecated("Skal erstattes med OPPHØR_FRA_FORRIGE_BEHANDLING, må endres i sanity")
    GJELDER_FØRSTE_PERIODE,

    @Deprecated("Skal erstattes med REDUKSJON_FRA_FORRIGE_BEHANDLING, må endres i sanity")
    GJELDER_FRA_INNVILGELSESTIDSPUNKT,
}

data class SanityEØSBegrunnelse(
    override val apiNavn: String,
    override val navnISystem: String,
    override val periodeResultat: SanityPeriodeResultat? = null,
    override val vilkår: Set<Vilkår>,
    override val fagsakType: FagsakType?,
    override val tema: Tema?,
    override val periodeType: BrevPeriodeType?,
    override val begrunnelseTypeForPerson: VedtakBegrunnelseType? = null,
    override val valgbarhet: Valgbarhet?,
    override val ovrigeTriggere: List<ØvrigTrigger> = emptyList(),
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
    override val borMedSokerTriggere: List<VilkårTrigger> = emptyList()
}

private fun SanityBegrunnelse.tilTriggesAv(): TriggesAv {
    return TriggesAv(
        vilkår = this.vilkaar.map { it.tilVilkår() }.toSet(),
        personTyper =
            if (this.rolle.isEmpty()) {
                when {
                    this.inneholderVilkår(SanityVilkår.BOSATT_I_RIKET) -> setOf(PersonType.BARN, PersonType.SØKER)
                    this.inneholderVilkår(SanityVilkår.LOVLIG_OPPHOLD) -> setOf(PersonType.BARN, PersonType.SØKER)
                    this.inneholderVilkår(SanityVilkår.GIFT_PARTNERSKAP) -> setOf(PersonType.BARN)
                    this.inneholderVilkår(SanityVilkår.UNDER_18_ÅR) -> setOf(PersonType.BARN)
                    this.inneholderVilkår(SanityVilkår.BOR_MED_SOKER) -> setOf(PersonType.BARN)
                    else -> setOf(PersonType.BARN, PersonType.SØKER)
                }
            } else {
                this.rolle.map { it.tilPersonType() }.toSet()
            },
        personerManglerOpplysninger = this.inneholderØvrigTrigger(ØvrigTrigger.MANGLER_OPPLYSNINGER),
        satsendring = this.inneholderØvrigTrigger(ØvrigTrigger.SATSENDRING),
        barnMedSeksårsdag = this.inneholderØvrigTrigger(ØvrigTrigger.BARN_MED_6_ÅRS_DAG),
        vurderingAnnetGrunnlag = (
            this.inneholderLovligOppholdTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderBosattIRiketTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderGiftPartnerskapTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderBorMedSøkerTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG)
        ),
        medlemskap = this.inneholderBosattIRiketTrigger(VilkårTrigger.MEDLEMSKAP),
        deltbosted = this.inneholderBorMedSøkerTrigger(VilkårTrigger.DELT_BOSTED),
        deltBostedSkalIkkeDeles = this.inneholderBorMedSøkerTrigger(VilkårTrigger.DELT_BOSTED_SKAL_IKKE_DELES),
        valgbar = !this.inneholderØvrigTrigger(ØvrigTrigger.ALLTID_AUTOMATISK),
        valgbarhet = this.valgbarhet,
        etterEndretUtbetaling =
            this.endretUtbetalingsperiodeTriggere
                .contains(EndretUtbetalingsperiodeTrigger.ETTER_ENDRET_UTBETALINGSPERIODE) ?: false,
        endretUtbetalingSkalUtbetales =
            this.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger
                ?: EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT,
        endringsaarsaker = this.endringsaarsaker.toSet(),
        småbarnstillegg = this.inneholderUtvidetBarnetrygdTrigger(UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG),
        gjelderFørstePeriode = this.inneholderØvrigTrigger(ØvrigTrigger.GJELDER_FØRSTE_PERIODE),
        gjelderFraInnvilgelsestidspunkt = this.inneholderØvrigTrigger(ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT),
        barnDød = this.inneholderØvrigTrigger(ØvrigTrigger.BARN_DØD),
    )
}
