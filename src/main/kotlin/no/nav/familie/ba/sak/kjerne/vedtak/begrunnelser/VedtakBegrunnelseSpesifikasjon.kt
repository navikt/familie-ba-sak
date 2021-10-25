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
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
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
        override val sanityApiNavn = "heleFamilienTrygdeavtale"
    },
    INNVILGET_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "heleFamilienPliktigMedlem"
    },
    INNVILGET_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "sokerOgBarnPliktigMedlem"
    },
    INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgelseEnighetOmAtAvtalenOmDeltBostedErOpphort"
    },
    INNVILGET_VURDERING_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "vurderingHeleFamilienFrivilligMedlem"
    },
    INNVILGET_UENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgelseUenighetOmOpphorAvAvtaleOmDeltBosted"
    },
    INNVILGET_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "heleFamilienFrivilligMedlem"
    },
    INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "vurderingHeleFamilienPliktigMedlem"
    },
    INNVILGET_SØKER_OG_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "sokerOgBarnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "sokerOgBarnFrivilligMedlem"
    },
    INNVILGET_VURDERING_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "vurderingSokerOgBarnFrivilligMedlem"
    },
    INNVILGET_ETTERBETALING_3_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "etterbetaling3Aar"
    },
    INNVILGET_SØKER_OG_BARN_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "sokerOgBarnTrygdeavtale"
    },
    INNVILGET_ALENE_FRA_FØDSEL {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetAleneFraFodsel"
    },
    INNVILGET_VURDERING_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "vurderingSokerOgBarnPliktigMedlem"
    },
    INNVILGET_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "barnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    INNVILGET_SATSENDRING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSatsendring"
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
        override val sanityApiNavn = "annenForelderIkkeMedlem"
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
        override val sanityApiNavn = "vurderingBarnFlereKorteOppholdIUtlandetSisteArene"
    },
    REDUKSJON_VURDERING_BARN_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR_ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "vurderingBarnFlereKorteOppholdIUtlandetSisteToAr"
    },
    REDUKSJON_SATSENDRING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSatsendring"
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
        override val sanityApiNavn = "ikkeMedlem"
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
        override val sanityApiNavn = "ikkeFrivilligMedlem"
    },
    AVSLAG_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "ikkePliktigMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkeMedlemEtterTrygdeavtale"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "annenForelderIkkePliktigMedlem"
    },
    AVSLAG_VURDERING_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "vurderingIkkeMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "annenForelderIkkeFrivilligMedlem"
    },
    AVSLAG_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "ikkeMedlemEtterTrygdeavtale"
    },
    AVSLAG_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingFlereKorteOppholdIUtlandetSisteToAar"
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
        override val sanityApiNavn = "annenForelderIkkeLengerPliktigMedlem"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "sokerOgBarnIkkeLengerPliktigMedlem"
    },
    OPPHØR_BOSATT_I_NORGE_UNNTATT_MEDLEMSKAP {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "bosattINorgeUnntattMedlemskap"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "annenForelderIkkeLengerMedlemTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "sokerOgBarnIkkeLengerMedlemTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "sokerOgBarnIkkeLengerFrivilligMedlem"
    },
    OPPHØR_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "vurderingAnnenForelderIkkeMedlem"
    },
    OPPHØR_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "vurderingFlereKorteOppholdIUtlandetSisteToAr"
    },
    OPPHØR_VURDERING_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "vurderingSokerOgBarnIkkeMedlem"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "sokerOgBarnIkkeMedlem"
    },
    OPPHØR_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "vurderingFlereKorteOppholdIUtlandetSisteArene"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "annenForelderIkkeLengerFrivilligMedlem"
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
        override val sanityApiNavn = "oppholdIUtlandetIkkeMerEnn3ManederSokerOgBarn"
    },
    FORTSATT_INNVILGET_HELE_FAMILIEN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "heleFamilienMedlemEtterTrygdeavtale"
    },
    FORTSATT_INNVILGET_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "oppholdIUtlandetIkkeMerEnn3ManederBarn"
    },
    FORTSATT_INNVILGET_DELT_BOSTED_PRAKTISERES_FORTSATT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetDeltBostedPraktiseresFortsatt"
    },
    FORTSATT_INNVILGET_VURDERING_HELE_FAMILIEN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "vurderingHeleFamilienMedlem"
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "sokerOgBarnMedlemEtterTrygdeavtale"
    },
    FORTSATT_INNVILGET_ANNEN_FORELDER_IKKE_SØKT_OM_DELT_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetAnnenForelderIkkeSokt"
    },
    FORTSATT_INNVILGET_VURDERING_SØKER_OG_BARN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "vurderingSokerOgBarnMedlem"
    },
    FORTSATT_INNVILGET_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemIFolketrygden"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingsperiodeDeltBostedFullUtbetaling"
    },
    ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.ENDRET_UTBETALING
        override val sanityApiNavn = "endretUtbetalingsperiodeDeltBostedIngenUtbetaling"
    },
    PERIODE_ETTER_ENDRET_UTBETALING_RETTSAVGJØRELSE_DELT_BOSTED {
        override val sanityApiNavn = "periodeEtterEndretUtbetalingsperiodeRettsavgjorelseDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
    },
    PERIODE_ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES {
        override val sanityApiNavn = "periodeEtterEndretUtbetalingsperiodeAvtaleDeltBostedFolges"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
    },
    PERIODE_ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED {
        override val sanityApiNavn = "periodeEtterEndringsperiodeHarAvtaleDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
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

        return when {
            !triggesAv.valgbar -> false
            !utvidetVedtaksperiodeMedBegrunnelser.type.tillatteBegrunnelsestyper.contains(vedtakBegrunnelseType) -> false
            triggesAv.personerManglerOpplysninger -> vilkårsvurdering.harPersonerManglerOpplysninger()
            triggesAv.barnMedSeksårsdag -> persongrunnlag.harBarnMedSeksårsdagPåFom(utvidetVedtaksperiodeMedBegrunnelser.fom)
            triggesAv.satsendring ->
                SatsService
                    .finnSatsendring(utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN)
                    .isNotEmpty()

            triggesAv.erEndret() -> erEtterEndretPeriode && triggesAv.etterEndretUtbetaling && utvidetVedtaksperiodeMedBegrunnelser.type != Vedtaksperiodetype.ENDRET_UTBETALING

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

    companion object {

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

fun VedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(sanityBegrunnelser: List<SanityBegrunnelse>): SanityBegrunnelse =
    sanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
        ?: throw Feil("Fant ikke begrunnelse med apiNavn=${this.sanityApiNavn} for ${this.name} i Sanity.")

fun VedtakBegrunnelseSpesifikasjon.erTilknyttetVilkår(sanityBegrunnelser: List<SanityBegrunnelse>) =
    !this.tilSanityBegrunnelse(sanityBegrunnelser).vilkaar.isNullOrEmpty()

enum class VedtakBegrunnelseType {
    INNVILGET,
    REDUKSJON,
    AVSLAG,
    OPPHØR,
    FORTSATT_INNVILGET,
    ENDRET_UTBETALING
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
