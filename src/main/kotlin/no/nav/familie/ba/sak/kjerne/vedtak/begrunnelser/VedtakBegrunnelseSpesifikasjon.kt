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
    val etterEndretUtbetaling: Boolean = false
)

enum class VedtakBegrunnelseSpesifikasjon : IVedtakBegrunnelse {

    PERIODE_ETTER_ENDRET_UTBETALING_RETTSAVGJØRELSE_DELT_BOSTED {
        override val sanityApiNavn = "periodeEtterEndretUtbetalingsperiodeRettsavgjorelseDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
    },
    PERIODE_ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES {
        override val sanityApiNavn = "periodeEtterEndretUtbetalingsperiodeAvtaleDeltBostedFolges"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
    },
    PERIODE_ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED {
        override val sanityApiNavn = "periodeEtterEndringsperiodeHarAvtaleDeltBosted"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
    },
    INNVILGET_BOSATT_I_RIKTET {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetBosattIRiket"
    },
    INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetBosattIRiketLovligOpphold"
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetLovligOppholdOppholdstillatelse"
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorger"
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorgerSkjonnsmessigVurdering"
    },
    INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
    },
    INNVILGET_OMSORG_FOR_BARN {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetOmsorgForBarn"
    },
    INNVILGET_BOR_HOS_SØKER {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetBorHosSoker"
    },
    INNVILGET_BOR_HOS_SØKER_SKJØNNSMESSIG {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetBorHosSokerSkjonnsmessig"
    },
    INNVILGET_FAST_OMSORG_FOR_BARN {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetFastOmsorgForBarn"
    },
    INNVILGET_NYFØDT_BARN_FØRSTE {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetNyfodtBarnForste"
    },
    INNVILGET_NYFØDT_BARN {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetNyfodtBarn"
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarnForste"
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarn"
    },

    INNVILGET_MEDLEM_I_FOLKETRYGDEN {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetMedlemIFolketrygden"
    },
    INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetBarnBorSammenMedMottaker"
    },
    INNVILGELSE_BEREDSKAPSHJEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgelseBeredskapshjem"
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
    INNVILGET_SATSENDRING {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgetSatsendring"
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
    INNVILGELSE_HELE_FAMILIEN_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "heleFamilienTrygdeavtale"
    },
    INNVILGELSE_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "heleFamilienPliktigMedlem"
    },
    INNVILGELSE_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "sokerOgBarnPliktigMedlem"
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
    INNVILGELSE_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgelseEnighetOmAtAvtalenOmDeltBostedErOpphort"
    },
    FORTSATT_INNVILGET_HELE_FAMILIEN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "heleFamilienMedlemEtterTrygdeavtale"
    },
    FORTSATT_INNVILGET_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "oppholdIUtlandetIkkeMerEnn3ManederBarn"
    },
    INNVILGELSE_VURDERING_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "vurderingHeleFamilienFrivilligMedlem"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "annenForelderIkkeLengerPliktigMedlem"
    },
    AVSLAG_IKKE_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "ikkeFrivilligMedlem"
    },
    INNVILGELSE_VURDERING_SØKER_OG_BARN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "vurderingSokerOgBarnPliktigMedlem"
    },
    FORTSATT_INNVILGET_MEDLEM_I_FOLKETRYGDEN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemIFolketrygden"
    },
    AVSLAG_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "ikkePliktigMedlem"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerFrivilligMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAnnenForelderIkkeMedlemEtterTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "sokerOgBarnIkkeLengerPliktigMedlem"
    },
    AVSLAG_ANNEN_FORELDER_IKKE_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "annenForelderIkkePliktigMedlem"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "annenForelderIkkeMedlem"
    },
    REDUKSJON_VURDERING_BARN_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE_ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "vurderingBarnFlereKorteOppholdIUtlandetSisteArene"
    },
    FORTSATT_INNVILGET_DELT_BOSTED_PRAKTISERES_FORTSATT {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetDeltBostedPraktiseresFortsatt"
    },
    INNVILGELSE_UENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "innvilgelseUenighetOmOpphorAvAvtaleOmDeltBosted"
    },
    INNVILGELSE_HELE_FAMILIEN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "heleFamilienFrivilligMedlem"
    },
    INNVILGELSE_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "vurderingHeleFamilienPliktigMedlem"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerMedlemTrygdeavtale"
    },
    OPPHØR_BOSATT_I_NORGE_UNNTATT_MEDLEMSKAP {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "bosattINorgeUnntattMedlemskap"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "annenForelderIkkeLengerMedlemTrygdeavtale"
    },
    REDUKSJON_ANNEN_FORELDER_IKKE_LENGER_PLIKTIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAnnenForelderIkkeLengerPliktigMedlem"
    },
    FORTSATT_INNVILGET_VURDERING_HELE_FAMILIEN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "vurderingHeleFamilienMedlem"
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "sokerOgBarnMedlemEtterTrygdeavtale"
    },
    AVSLAG_VURDERING_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "vurderingIkkeMedlem"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_LENGER_MEDLEM_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "sokerOgBarnIkkeLengerMedlemTrygdeavtale"
    },
    INNVILGELSE_SØKER_OG_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "sokerOgBarnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    REDUKSJON_VURDERING_BARN_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR_ {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "vurderingBarnFlereKorteOppholdIUtlandetSisteToAr"
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
    AVSLAG_ANNEN_FORELDER_IKKE_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "annenForelderIkkeFrivilligMedlem"
    },
    OPPHØR_VURDERING_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "vurderingSokerOgBarnIkkeMedlem"
    },
    AVSLAG_IKKE_MEDLEM_ETTER_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "ikkeMedlemEtterTrygdeavtale"
    },
    OPPHØR_SØKER_OG_BARN_IKKE_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "sokerOgBarnIkkeMedlem"
    },
    AVSLAG_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_TO_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingFlereKorteOppholdIUtlandetSisteToAar"
    },
    INNVILGELSE_BARN_OPPHOLD_I_UTLANDET_IKKE_MER_ENN_3_MÅNEDER {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "barnOppholdIUtlandetIkkeMerEnn3Maneder"
    },
    INNVILGELSE_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "sokerOgBarnFrivilligMedlem"
    },
    OPPHØR_VURDERING_FLERE_KORTE_OPPHOLD_I_UTLANDET_SISTE_ÅRENE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "vurderingFlereKorteOppholdIUtlandetSisteArene"
    },
    INNVILGELSE_VURDERING_SØKER_OG_BARN_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "vurderingSokerOgBarnFrivilligMedlem"
    },
    INNVILGELSE_ETTERBETALING_3_ÅR {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "etterbetaling3Aar"
    },
    FORTSATT_INNVILGET_ANNEN_FORELDER_IKKE_SØKT_OM_DELT_BARNETRYGD {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetAnnenForelderIkkeSokt"
    },
    INNVILGELSE_SØKER_OG_BARN_TRYGDEAVTALE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override val sanityApiNavn = "sokerOgBarnTrygdeavtale"
    },
    FORTSATT_INNVILGET_VURDERING_SØKER_OG_BARN_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "vurderingSokerOgBarnMedlem"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_LENGER_FRIVILLIG_MEDLEM {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override val sanityApiNavn = "annenForelderIkkeLengerFrivilligMedlem"
    },
    REDUKSJON_SATSENDRING {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSatsendring"
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
        if (!triggesAv.valgbar) return false

        if (utvidetVedtaksperiodeMedBegrunnelser.type != vedtakBegrunnelseType.tilVedtaksperiodeType()) return false

        if (triggesAv.personerManglerOpplysninger) return vilkårsvurdering.harPersonerManglerOpplysninger()

        if (triggesAv.barnMedSeksårsdag)
            return persongrunnlag.harBarnMedSeksårsdagPåFom(utvidetVedtaksperiodeMedBegrunnelser.fom)

        if (triggesAv.satsendring)
            return SatsService
                .finnSatsendring(utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN)
                .isNotEmpty()

        val aktuellePersoner = persongrunnlag.personer
            .filter { person -> triggesAv.personTyper.contains(person.type) }
            .filter { person ->
                if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE) {
                    identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
                } else true
            }

        if (triggesAv.etterEndretUtbetaling)
            return erEtterEndretPeriodeAvSammeÅrsak(
                endretUtbetalingAndeler,
                utvidetVedtaksperiodeMedBegrunnelser,
                aktuellePersoner,
                triggesAv
            )

        return hentPersonerForAlleUtgjørendeVilkår(
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

    companion object {

        fun List<LocalDate>.tilBrevTekst(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })
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
    INNVILGELSE,
    REDUKSJON,
    AVSLAG,
    OPPHØR,
    FORTSATT_INNVILGET
}

fun VedtakBegrunnelseSpesifikasjon.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    personIdenter: List<String>
): Vedtaksbegrunnelse {
    if (this.vedtakBegrunnelseType.tilVedtaksperiodeType() != vedtaksperiodeMedBegrunnelser.type) {
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

fun VedtakBegrunnelseType.tilVedtaksperiodeType() = when (this) {
    VedtakBegrunnelseType.INNVILGELSE, VedtakBegrunnelseType.REDUKSJON -> Vedtaksperiodetype.UTBETALING
    VedtakBegrunnelseType.AVSLAG -> Vedtaksperiodetype.AVSLAG
    VedtakBegrunnelseType.OPPHØR -> Vedtaksperiodetype.OPPHØR
    VedtakBegrunnelseType.FORTSATT_INNVILGET -> Vedtaksperiodetype.FORTSATT_INNVILGET
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
