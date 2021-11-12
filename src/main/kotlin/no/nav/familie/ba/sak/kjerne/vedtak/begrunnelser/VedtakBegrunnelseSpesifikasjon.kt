package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.konverterEnumsTilString
import no.nav.familie.ba.sak.common.Utils.konverterStringTilEnums
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.beregning.fomErPåSatsendring
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.AttributeConverter
import javax.persistence.Converter

interface IVedtakBegrunnelse {

    val sanityApiNavn: String
    val vedtakBegrunnelseType: VedtakBegrunnelseType
}

data class TriggesAv(
    val vilkår: Set<Vilkår> = emptySet(),
    val personTyper: Set<PersonType> = setOf(PersonType.BARN, PersonType.SØKER),
    val personerManglerOpplysninger: Boolean = false,
    val satsendring: Boolean = false,
    val barnMedSeksårsdag: Boolean = false,
    val vurderingAnnetGrunnlag: Boolean = false,
    val medlemskap: Boolean = false,
    val deltbosted: Boolean = false,
    val valgbar: Boolean = true,
    val endringsaarsaker: Set<Årsak> = emptySet(),
    val etterEndretUtbetaling: Boolean = false,
    val endretUtbetaingSkalUtbetales: Boolean = false
) {
    fun erEndret() = endringsaarsaker.isNotEmpty()
}

enum class VedtakBegrunnelseSpesifikasjon : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBosattIRiket"
    },
    INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBosattIRiketLovligOpphold"
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdOppholdstillatelse"
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorger"
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorgerSkjonnsmessigVurdering"
    },
    INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
    },
    INNVILGET_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOmsorgForBarn"
    },
    INNVILGET_BOR_HOS_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorHosSoker"
    },
    INNVILGET_BOR_HOS_SØKER_SKJØNNSMESSIG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorHosSokerSkjonnsmessig"
    },
    INNVILGET_FAST_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFastOmsorgForBarn"
    },
    INNVILGET_NYFØDT_BARN_FØRSTE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetNyfodtBarnForste"
    },
    INNVILGET_NYFØDT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetNyfodtBarn"
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarnForste"
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarn"
    },
    INNVILGET_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMedlemIFolketrygden"
    },
    INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarnBorSammenMedMottaker"
    },
    INNVILGET_BEREDSKAPSHJEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBeredskapshjem"
    },
    INNVILGET_HELE_FAMILIEN_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetHeleFamilienTrygdeavtale"
    },
    INNVILGET_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetHeleFamilienPliktigMedlem"
    },
    INNVILGET_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnPliktigMedlem"
    },
    INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEnighetOmAtAvtalenOmDeltBostedErOpphort"
    },
    INNVILGET_VURDERING_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingHeleFamilienFrivilligMedlem"
    },
    INNVILGET_UENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetUenighetOmOpphorAvAvtaleOmDeltBosted"
    },
    INNVILGET_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetHeleFamilienFrivilligMedlem"
    },
    INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingHeleFamilienPliktigMedlem"
    },
    INNVILGET_SØKER_OG_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnFrivilligMedlem"
    },
    INNVILGET_VURDERING_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingSokerOgBarnFrivilligMedlem"
    },
    INNVILGET_ETTERBETALING_3_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEtterbetaling3Aar"
    },
    INNVILGET_SØKER_OG_BARN_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSokerOgBarnTrygdeavtale"
    },
    INNVILGET_ALENE_FRA_FØDSEL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAleneFraFodsel"
    },
    INNVILGET_VURDERING_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingSokerOgBarnPliktigMedlem"
    },
    INNVILGET_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    INNVILGET_SATSENDRING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSatsendring"
    },
    INNVILGET_FLYTTET_ETTER_SEPARASJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFlyttetEtterSeparasjon"
    },
    INNVILGET_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSeparert"
    },
    INNVILGET_VARETEKTSFENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVaretektsfengselSamboer"
    },
    INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_FLYTTETIDSPUNKT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAvtaleDeltBostedFaarFraFlyttetidspunkt"
    },
    INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTvungentPsykiskHelsevernGift"
    },
    INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTvungentPsykiskHelsevernSamboer"
    },
    INNVILGET_FENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFengselGift"
    },
    INNVILGET_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingEgenHusholdning"
    },
    INNVILGET_FORSVUNNET_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForsvunnetSamboer"
    },
    INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAvtaleDeltBostedFaarFraAvtaletidspunkt"
    },
    INNVILGET_FORVARING_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForvaringGift"
    },
    INNVILGET_MEKLINGSATTEST_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMeklingsattestOgVurderingEgenHusholdning"
    },
    INNVILGET_FENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFengselSamboer"
    },
    INNVILGET_FLYTTING_ETTER_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFlyttingEtterMeklingsattest"
    },
    INNVILGET_FORVARING_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForvaringSamboer"
    },
    INNVILGET_SEPARERT_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSeparertOgVurderingEgenHusholdning"
    },
    INNVILGET_BARN_16_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarn16Ar"
    },
    INNVILGET_SAMBOER_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSamboerDod"
    },
    INNVILGET_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMeklingsattest"
    },
    INNVILGET_FLYTTET_ETTER_SKILT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFlyttetEtterSkilt"
    },
    INNVILGET_ENSLIG_MINDREÅRIG_FLYKTNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEnsligMindrearigFlyktning"
    },
    INNVILGET_VARETEKTSFENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVaretektsfengselGift"
    },
    INNVILGET_SAMBOER_UTEN_FELLES_BARN_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSamboerUtenFellesBarnOgVurderingEgenHusholdning"
    },
    INNVILGET_FORSVUNNET_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForsvunnetEktefelle"
    },
    INNVILGET_FAKTISK_SEPARASJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFaktiskSeparasjon"
    },
    INNVILGET_SAMBOER_UTEN_FELLES_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSamboerUtenFellesBarn"
    },
    INNVILGET_VURDERING_AVTALE_DELT_BOSTED_FØLGES {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingAvtaleDeltBostedFolges"
    },
    INNVILGET_SKILT_OG_VURDERING_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSkiltOgVurderingEgenHusholdning"
    },
    INNVILGET_BOR_ALENE_MED_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorAleneMedBarn"
    },
    INNVILGET_SKILT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSkilt"
    },
    INNVILGET_RETTSAVGJØRELSE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetRettsavgjorelseDeltBosted"
    },
    INNVILGET_EKTEFELLE_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetEktefelleDod"
    },
    INNVILGET_SMÅBARNSTILLEGG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSmaabarnstillegg"
    },

    REDUKSJON_BOSATT_I_RIKTET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBosattIRiket"
    },
    REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonLovligOppholdOppholdstillatelseBarn"
    },
    REDUKSJON_FLYTTET_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFlyttetBarn"
    },
    REDUKSJON_BARN_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnDod"
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFastOmsorgForBarn"
    },
    REDUKSJON_MANGLENDE_OPPLYSNINGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonManglendeOpplysninger"
    },
    REDUKSJON_UNDER_18_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonUnder18Aar"
    },
    REDUKSJON_UNDER_6_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonUnder6Aar"
    },
    REDUKSJON_DELT_BOSTED_ENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBostedEnighet"
    },
    REDUKSJON_DELT_BOSTED_UENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDeltBostedUenighet"
    },
    REDUKSJON_ENDRET_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEndretMottaker"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerFrivilligMedlem"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeMedlem"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerMedlemTrygdeavtale"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerPliktigMedlem"
    },
    REDUKSJON_VURDERING_BARN_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE_ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingBarnFlereKorteOppholdIUtlandetSisteArene"
    },
    REDUKSJON_VURDERING_BARN_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR_ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingBarnFlereKorteOppholdIUtlandetSisteToAr"
    },
    REDUKSJON_SATSENDRING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSatsendring"
    },
    REDUKSJON_NYFØDT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonNyfodtBarn"
    },
    REDUKSJON_VURDERING_SØKER_GIFTET_SEG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingSokerGiftetSeg"
    },
    REDUKSJON_VURDERING_SAMBOER_MER_ENN_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingSamboerMerEnn12Maaneder"
    },
    REDUKSJON_AVTALE_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAvtaleFastBosted"
    },
    REDUKSJON_EKTEFELLE_IKKE_I_FENGSEL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeIFengsel"
    },
    REDUKSJON_SAMBOER_MER_ENN_12_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerMerEnn12Maaneder"
    },
    REDUKSJON_SAMBOER_IKKE_I_TVUNGENT_PSYKISK_HELSEVERN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeITvungentPsykiskHelsevern"
    },
    REDUKSJON_SAMBOER_IKKE_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeEgenHusholdning"
    },
    REDUKSJON_SAMBOER_IKKE_I_FENGSEL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeIFengsel"
    },
    REDUKSJON_VURDERING_FLYTTET_SAMMEN_MED_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingFlyttetSammenMedEktefelle"
    },
    REDUKSJON_VURDERING_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingForeldreneBorSammen"
    },
    REDUKSJON_SAMBOER_IKKE_I_FORVARING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeIForvaring"
    },
    REDUKSJON_EKTEFELLE_IKKE_I_TVUNGENT_PSYKISK_HELSEVERN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeITvungentPsykiskHelsevern"
    },
    REDUKSJON_EKTEFELLE_IKKE_I_FORVARING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeIForvaring"
    },
    REDUKSJON_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonForeldreneBorSammen"
    },
    REDUKSJON_EKTEFELLE_IKKE_LENGER_FORSVUNNET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonEktefelleIkkeLengerForsvunnet"
    },
    REDUKSJON_RETTSAVGJØRELSE_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonRettsavgjorelseFastBosted"
    },
    REDUKSJON_FLYTTET_SAMMEN_MED_ANNEN_FORELDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFlyttetSammenMedAnnenForelder"
    },
    REDUKSJON_GIFT_IKKE_EGEN_HUSHOLDNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonGiftIkkeEgenHusholdning"
    },
    REDUKSJON_FLYTTET_SAMMEN_MED_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFlyttetSammenMedEktefelle"
    },
    REDUKSJON_IKKE_AVTALE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonIkkeAvtaleDeltBosted"
    },
    REDUKSJON_SØKER_GIFTER_SEG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSokerGifterSeg"
    },
    REDUKSJON_SAMBOER_IKKE_LENGER_FORSVUNNET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSamboerIkkeLengerForsvunnet"
    },
    REDUKSJON_VURDERING_FLYTTET_SAMMEN_MED_ANNEN_FORELDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonVurderingFlyttetSammenMedAnnenForelder"
    },
    AVSLAG_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBosattIRiket"
    },
    AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagLovligOppholdTredjelandsborger"
    },
    AVSLAG_BOR_HOS_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBorHosSoker"
    },
    AVSLAG_OMSORG_FOR_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagOmsorgForBarn"
    },
    AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagLovligOppholdEosBorger"
    },
    AVSLAG_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
    },
    AVSLAG_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagMedlemIFolketrygden"
    },
    AVSLAG_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagForeldreneBorSammen"
    },
    AVSLAG_UNDER_18_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUnder18Aar"
    },
    AVSLAG_UGYLDIG_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUgyldigAvtaleOmDeltBosted"
    },
    AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeAvtaleOmDeltBosted"
    },
    AVSLAG_OPPLYSNINGSPLIKT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagOpplysningsplikt"
    },
    AVSLAG_SÆRKULLSBARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSaerkullsbarn"
    },
    AVSLAG_UREGISTRERT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUregistrertBarn"
    },
    AVSLAG_IKKE_DOKUMENTERT_BOSATT_I_NORGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertBosattINorge"
    },
    AVSLAG_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMedlem"
    },
    AVSLAG_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingFlereKorteOppholdIUtlandetSisteArene"
    },
    AVSLAG_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingAnnenForelderIkkeMedlem"
    },
    AVSLAG_IKKE_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeFrivilligMedlem"
    },
    AVSLAG_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkePliktigMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkeMedlemEtterTrygdeavtale"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkePliktigMedlem"
    },
    AVSLAG_VURDERING_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkeFrivilligMedlem"
    },
    AVSLAG_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMedlemEtterTrygdeavtale"
    },
    AVSLAG_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingFlereKorteOppholdIUtlandetSisteToAar"
    },
    AVSLAG_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboer"
    },
    AVSLAG_SAMBOER_IKKE_FLYTTET_FRA_HVERANDRE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboerIkkeFlyttetFraHverandre"
    },
    AVSLAG_BARN_HAR_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBarnHarFastBosted"
    },
    AVSLAG_IKKE_EGEN_HUSHOLDNING_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeEgenHusholdningSamboer"
    },
    AVSLAG_GIFT_MIDLERTIDIG_ADSKILLELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagGiftMidlertidigAdskillelse"
    },
    AVSLAG_IKKE_EGEN_HUSHOLDNING_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeEgenHusholdningGift"
    },
    AVSLAG_MANGLER_AVTALE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagManglerAvtaleDeltBosted"
    },
    AVSLAG_VURDERING_IKKE_FLYTTET_FRA_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeFlyttetFraEktefelle"
    },
    AVSLAG_RETTSAVGJØRELSE_SAMVÆR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagRettsavgjorelseSamver"
    },
    AVSLAG_IKKE_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeSeparert"
    },
    AVSLAG_FENGSEL_UNDER_6_MÅNEDER_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFengselUnder6MaanederEktefelle"
    },
    AVSLAG_IKKE_DOKUMENTERT_SKILT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertSkilt"
    },
    AVSLAG_VURDERING_IKKE_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeMeklingsattest"
    },
    AVSLAG_FORVARING_UNDER_6_MÅNEDER_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagForvaringUnder6MaanederEktefelle"
    },
    AVSLAG_EKTEFELLE_FORSVUNNET_MINDRE_ENN_6_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagEktefelleForsvunnetMindreEnn6Maaneder"
    },
    AVSLAG_VURDERING_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingForeldreneBorSammen"
    },
    AVSLAG_SAMBOER_MIDLERTIDIG_ADSKILLELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboerMidlertidigAdskillelse"
    },
    AVSLAG_IKKE_FLYTTET_FRA_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeFlyttetFraEktefelle"
    },
    AVSLAG_IKKE_MEKLINGSATTEST {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMeklingsattest"
    },
    AVSLAG_FORVARING_UNDER_6_MÅNEDER_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagForvaringUnder6MaanederSamboer"
    },
    AVSLAG_IKKE_DOKUMENTERT_EKTEFELLE_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertEktefelleDod"
    },
    AVSLAG_VURDERING_IKKE_TVUNGENT_PSYKISK_HELSEVERN_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeTvungentPsykiskHelsevernEktefelle"
    },
    AVSLAG_VURDERING_IKKE_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeSeparert"
    },
    AVSLAG_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagGift"
    },
    AVSLAG_SAMBOER_FORSVUNNET_MINDRE_ENN_6_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSamboerForsvunnetMindreEnn6Maaneder"
    },
    AVSLAG_VURDERING_IKKE_TVUNGENT_PSYKISK_HELSEVERN_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeTvungentPsykiskHelsevernSamboer"
    },
    AVSLAG_VURDERING_SAMBOER_IKKE_FLYTTET_FRA_HVERANDRE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingSamboerIkkeFlyttetFraHverandre"
    },
    AVSLAG_ENSLIG_MINDREÅRIG_FLYKTNING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagEnsligMindreaarigFlyktning"
    },
    AVSLAG_IKKE_DELT_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDeltForeldreneBorSammen"
    },
    AVSLAG_IKKE_GYLDIG_AVTALE_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeGyldigAvtaleDeltBosted"
    },
    AVSLAG_FENGSEL_UNDER_6_MÅNEDER_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFengselUnder6MaanederSamboer"
    },
    AVSLAG_IKKE_DOKUMENTERT_SAMBOER_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeDokumentertSamboerDod"
    },
    OPPHØR_BARN_FLYTTET_FRA_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnBorIkkeMedSoker"
    },
    OPPHØR_UTVANDRET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFlyttetFraNorge"
    },
    OPPHØR_BARN_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorEtBarnErDodt"
    },
    OPPHØR_FLERE_BARN_DØD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFlereBarnErDode"
    },
    OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerHarIkkeFastOmsorg"
    },
    OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorHarIkkeOppholdstillatelse"
    },
    OPPHØR_IKKE_MOTTATT_OPPLYSNINGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeMottattOpplysninger"
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_ENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorDeltBostedOpphortEnighet"
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorDeltBostedOpphortUenighet"
    },
    OPPHØR_UNDER_18_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorUnder18Aar"
    },
    OPPHØR_ENDRET_MOTTAKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorEndretMottaker"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeLengerPliktigMedlem"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeLengerPliktigMedlem"
    },
    OPPHØR_BOSATT_I_NORGE_UNNTATT_MEDLEMSKAP {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBosattINorgeUnntattMedlemskap"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeLengerMedlemTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeLengerMedlemTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeLengerFrivilligMedlem"
    },
    OPPHØR_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingAnnenForelderIkkeMedlem"
    },
    OPPHØR_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingFlereKorteOppholdIUtlandetSisteToAr"
    },
    OPPHØR_VURDERING_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingSokerOgBarnIkkeMedlem"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerOgBarnIkkeMedlem"
    },
    OPPHØR_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingFlereKorteOppholdIUtlandetSisteArene"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeLengerFrivilligMedlem"
    },
    OPPHØR_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorForeldreneBorSammen"
    },
    OPPHØR_AVTALE_OM_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAvtaleOmFastBosted"
    },
    OPPHØR_RETTSAVGJØRELSE_FAST_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorRettsavgjorelseFastBosted"
    },
    OPPHØR_IKKE_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeAvtaleOmDeltBosted"
    },
    OPPHØR_VURDERING_FORELDRENE_BOR_SAMMEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingForeldreneBorSammen"
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerOgBarnBosattIRiket"
    },
    FORTSATT_INNVILGET_SØKER_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerBosattIRiket"
    },
    FORTSATT_INNVILGET_BARN_BOSATT_I_RIKET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBarnBosattIRiket"
    },
    FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBarnOgSokerLovligOppholdOppholdstillatelse"
    },
    FORTSATT_INNVILGET_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerLovligOppholdOppholdstillatelse"
    },
    FORTSATT_INNVILGET_BARN_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBarnLovligOppholdOppholdstillatelse"
    },
    FORTSATT_INNVILGET_BOR_MED_SØKER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBorMedSoker"
    },
    FORTSATT_INNVILGET_FAST_OMSORG {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFastOmsorg"
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_EØS {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetLovligOppholdEOS"
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_TREDJELANDSBORGER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetLovligOppholdTredjelandsborger"
    },
    FORTSATT_INNVILGET_UENDRET_TRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetUendretTrygd"
    },
    FORTSATT_INNVILGET_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER_SØKER_OG_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppholdIUtlandetIkkeMerEnn3ManederSokerOgBarn"
    },
    FORTSATT_INNVILGET_HELE_FAMILIEN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetHeleFamilienMedlemEtterTrygdeavtale"
    },
    FORTSATT_INNVILGET_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppholdIUtlandetIkkeMerEnn3ManederBarn"
    },
    FORTSATT_INNVILGET_DELT_BOSTED_PRAKTISERES_FORTSATT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetDeltBostedPraktiseresFortsatt"
    },
    FORTSATT_INNVILGET_VURDERING_HELE_FAMILIEN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVurderingHeleFamilienMedlem"
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSokerOgBarnMedlemEtterTrygdeavtale"
    },
    FORTSATT_INNVILGET_ANNEN_FORELDER_IKKE_SØKT_OM_DELT_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetAnnenForelderIkkeSokt"
    },
    FORTSATT_INNVILGET_VURDERING_SØKER_OG_BARN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVurderingSokerOgBarnMedlem"
    },
    FORTSATT_INNVILGET_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemIFolketrygden"
    },
    FORTSATT_INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetTvungentPsykiskHelsevernGift"
    },
    FORTSATT_INNVILGET_FENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFengselGift"
    },
    FORTSATT_INNVILGET_VURDERING_BOR_ALENE_MED_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVurderingBorAleneMedBarn"
    },
    FORTSATT_INNVILGET_SEPARERT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetSeparert"
    },
    FORTSATT_INNVILGET_FORTSATT_RETTSAVGJØRELSE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFortsattRettsavgjorelseOmDeltBosted"
    },
    FORTSATT_INNVILGET_BOR_ALENE_MED_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBorAleneMedBarn"
    },
    FORTSATT_INNVILGET_TVUNGENT_PSYKISK_HELSEVERN_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetTvungentPsykiskHelsevernSamboer"
    },
    FORTSATT_INNVILGET_FORVARING_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForvaringSamboer"
    },
    FORTSATT_INNVILGET_FORTSATT_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFortsattAVtaleOmDeltBosted"
    },
    FORTSATT_INNVILGET_VARETEKTSFENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVaretektsfengselSamboer"
    },
    FORTSATT_INNVILGET_FENGSEL_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFengselSamboer"
    },
    FORTSATT_INNVILGET_FORVARING_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForvaringGift"
    },
    FORTSATT_INNVILGET_VAREKTEKTSFENGSEL_GIFT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVaretektsfengselGift"
    },
    FORTSATT_INNVILGET_FORSVUNNET_SAMBOER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForsvunnetSamboer"
    },
    FORTSATT_INNVILGET_FORSVUNNET_EKTEFELLE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetForsvunnetEktefelle"
    },
    ENDRET_UTBETALING_DELT_BOSTED_FULL_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingDeltBostedFullUtbetalingForSoknad"
    },
    ENDRET_UTBETALING_DELT_BOSTED_INGEN_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingDeltBostedIngenUtbetalingForSoknad"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALING_UTVIDET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingDeltBostedKunEtterbetalingUtvidet"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_ORDINÆR_OG_ETTERBETALING_UTVIDET {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingDeltBostedFullOrdinarOgEtterbetalingUtvidet"
    },
    ETTER_ENDRET_UTBETALING_RETTSAVGJØRELSE_DELT_BOSTED {
        override val sanityApiNavn = "etterEndretUtbetalingRettsavgjorelseDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES {
        override val sanityApiNavn = "etterEndretUtbetalingVurderingAvtaleDeltBostedFolges"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    },
    ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED {
        override val sanityApiNavn = "etterEndretUtbetalingAvtaleDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
    };

    fun triggesForPeriode(
        utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
        vilkårsvurdering: Vilkårsvurdering,
        persongrunnlag: PersonopplysningGrunnlag,
        identerMedUtbetaling: List<String>,
        triggesAv: TriggesAv,
        vedtakBegrunnelseType: VedtakBegrunnelseType = this.vedtakBegrunnelseType,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList()
    ): Boolean {
        val aktuellePersoner = persongrunnlag.personer
            .filter { person -> triggesAv.personTyper.contains(person.type) }
            .filter { person ->
                if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
                    identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
                } else true
            }

        val erEtterEndretPeriode = erEtterEndretPeriodeAvSammeÅrsak(
            endretUtbetalingAndeler,
            utvidetVedtaksperiodeMedBegrunnelser,
            aktuellePersoner,
            triggesAv
        )

        val begrunnelseErRiktigType =
            utvidetVedtaksperiodeMedBegrunnelser.type.tillatteBegrunnelsestyper.contains(vedtakBegrunnelseType)
        return when {
            !triggesAv.valgbar -> false
            !begrunnelseErRiktigType -> false
            triggesAv.personerManglerOpplysninger -> vilkårsvurdering.harPersonerManglerOpplysninger()
            triggesAv.barnMedSeksårsdag ->
                persongrunnlag.harBarnMedSeksårsdagPåFom(utvidetVedtaksperiodeMedBegrunnelser.fom)
            triggesAv.satsendring -> fomErPåSatsendring(utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN)

            triggesAv.erEndret() ->
                erEtterEndretPeriode &&
                    triggesAv.etterEndretUtbetaling &&
                    utvidetVedtaksperiodeMedBegrunnelser.type != Vedtaksperiodetype.ENDRET_UTBETALING

            else -> hentPersonerForAlleUtgjørendeVilkår(
                vilkårsvurdering = vilkårsvurdering,
                vedtaksperiode = Periode(
                    fom = utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                    tom = utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
                ),
                oppdatertBegrunnelseType = vedtakBegrunnelseType,
                aktuellePersonerForVedtaksperiode = aktuellePersoner,
                triggesAv = triggesAv
            ).isNotEmpty()
        }
    }

    fun tilSanityBegrunnelse(
        sanityBegrunnelser: List<SanityBegrunnelse>
    ): SanityBegrunnelse? {
        val sanityBegrunnelse = sanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
        if (sanityBegrunnelse == null) {
            logger.warn("Finner ikke begrunnelse med apinavn '${this.sanityApiNavn}' på '${this.name}' i Sanity")
        }
        return sanityBegrunnelse
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakBegrunnelseSpesifikasjon::class.java)

        fun List<LocalDate>.tilBrevTekst(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })
    }
}

fun triggesAvSkalUtbetales(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    triggesAv: TriggesAv
): Boolean {
    if (triggesAv.etterEndretUtbetaling) return false

    val inneholderAndelSomSkalUtbetales = endretUtbetalingAndeler.any { it.prosent!! != BigDecimal.ZERO }
    val inneholderAndelSomIkkeSkalUtbetales = endretUtbetalingAndeler.any { it.prosent!! == BigDecimal.ZERO }

    return if (triggesAv.endretUtbetaingSkalUtbetales) {
        inneholderAndelSomSkalUtbetales
    } else {
        inneholderAndelSomIkkeSkalUtbetales
    }
}

private fun erEtterEndretPeriodeAvSammeÅrsak(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    aktuellePersoner: List<Person>,
    triggesAv: TriggesAv
) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
    endretUtbetalingAndel.tom!!.sisteDagIInneværendeMåned()
        .erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom) &&
        aktuellePersoner.any { person -> person.personIdent == endretUtbetalingAndel.person?.personIdent } &&
        triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}

val hjemlerTilhørendeFritekst = setOf(2, 4, 11)

fun VedtakBegrunnelseSpesifikasjon.erTilknyttetVilkår(sanityBegrunnelser: List<SanityBegrunnelse>): Boolean =
    !this.tilSanityBegrunnelse(sanityBegrunnelser)?.vilkaar.isNullOrEmpty()

enum class VedtakBegrunnelseType {
    INNVILGET,
    REDUKSJON,
    AVSLAG,
    OPPHØR,
    FORTSATT_INNVILGET,
    ENDRET_UTBETALING,
    ETTER_ENDRET_UTBETALING
}

fun VedtakBegrunnelseSpesifikasjon.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    personIdenter: List<String>
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser.type.tillatteBegrunnelsestyper.contains(this.vedtakBegrunnelseType)) {
        throw Feil(
            "Begrunnelsestype ${this.vedtakBegrunnelseType} passer ikke med " +
                "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden."
        )
    }

    return Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        vedtakBegrunnelseSpesifikasjon = this,
        personIdenter = personIdenter
    )
}

fun VedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(periode: Periode) = when (this) {
    VedtakBegrunnelseType.AVSLAG ->
        if (periode.fom == TIDENES_MORGEN && periode.tom == TIDENES_ENDE) ""
        else if (periode.tom == TIDENES_ENDE) periode.fom.tilMånedÅr()
        else "${periode.fom.tilMånedÅr()} til ${periode.tom.tilMånedÅr()}"
    else ->
        if (periode.fom == TIDENES_MORGEN)
            throw Feil("Prøver å finne fom-dato for begrunnelse, men fikk \"TIDENES_MORGEN\".")
        else periode.fom.forrigeMåned().tilMånedÅr()
}

@Converter
class VedtakBegrunnelseSpesifikasjonListConverter :
    AttributeConverter<List<VedtakBegrunnelseSpesifikasjon>, String> {

    override fun convertToDatabaseColumn(vedtakBegrunnelseSpesifikasjoner: List<VedtakBegrunnelseSpesifikasjon>) =
        konverterEnumsTilString(vedtakBegrunnelseSpesifikasjoner)

    override fun convertToEntityAttribute(string: String?): List<VedtakBegrunnelseSpesifikasjon> =
        konverterStringTilEnums(string)
}
