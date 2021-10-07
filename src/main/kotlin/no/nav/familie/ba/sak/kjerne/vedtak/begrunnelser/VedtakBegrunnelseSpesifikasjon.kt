package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.konverterEnumsTilString
import no.nav.familie.ba.sak.common.Utils.konverterStringTilEnums
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår
import no.nav.familie.ba.sak.kjerne.dokument.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.dokument.domene.inneholderBorMedSøkerTrigger
import no.nav.familie.ba.sak.kjerne.dokument.domene.inneholderBosattIRiketTrigger
import no.nav.familie.ba.sak.kjerne.dokument.domene.inneholderGiftPartnerskapTrigger
import no.nav.familie.ba.sak.kjerne.dokument.domene.inneholderLovligOppholdTrigger
import no.nav.familie.ba.sak.kjerne.dokument.domene.inneholderVilkår
import no.nav.familie.ba.sak.kjerne.dokument.domene.inneholderØvrigTrigger
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilPersonType
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilVilkår
import no.nav.familie.ba.sak.kjerne.dokument.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
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
    val vilkår: Set<Vilkår>? = null,
    val personTyper: Set<PersonType> = setOf(PersonType.BARN, PersonType.SØKER),
    val personerManglerOpplysninger: Boolean = false,
    val satsendring: Boolean = false,
    val barnMedSeksårsdag: Boolean = false,
    val vurderingAnnetGrunnlag: Boolean = false,
    val medlemskap: Boolean = false,
    val deltbosted: Boolean = false,
    val valgbar: Boolean = true
)

enum class VedtakBegrunnelseSpesifikasjon : IVedtakBegrunnelse {

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
    };

    fun triggesForPeriode(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        vilkårsvurdering: Vilkårsvurdering,
        persongrunnlag: PersonopplysningGrunnlag,
        identerMedUtbetaling: List<String>,
        triggesAv: TriggesAv,
        vedtakBegrunnelseType: VedtakBegrunnelseType = this.vedtakBegrunnelseType,
    ): Boolean {
        if (!triggesAv.valgbar) return false

        if (vedtaksperiodeMedBegrunnelser.type != vedtakBegrunnelseType.tilVedtaksperiodeType()) return false

        if (triggesAv.personerManglerOpplysninger) return vilkårsvurdering.harPersonerManglerOpplysninger()

        if (triggesAv.barnMedSeksårsdag)
            return persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom)

        if (triggesAv.satsendring)
            return SatsService
                .finnSatsendring(vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN)
                .isNotEmpty()

        return hentPersonerForAlleUtgjørendeVilkår(
            vilkårsvurdering = vilkårsvurdering,
            vedtaksperiode = Periode(
                fom = vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                tom = vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
            ),
            oppdatertBegrunnelseType = vedtakBegrunnelseType,
            utgjørendeVilkår = triggesAv.vilkår,
            aktuellePersonerForVedtaksperiode = persongrunnlag.personer
                .filter { person -> triggesAv.personTyper.contains(person.type) }
                .filter { person ->
                    if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE) {
                        identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
                    } else true
                },
            deltBosted = triggesAv.deltbosted,
            vurderingAnnetGrunnlag = triggesAv.vurderingAnnetGrunnlag
        ).isNotEmpty()
    }

    companion object {

        fun List<LocalDate>.tilBrevTekst(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })
    }
}

val hjemlerTilhørendeFritekst = setOf(2, 4, 11)

fun VedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(sanityBegrunnelser: List<SanityBegrunnelse>)
    : SanityBegrunnelse =
    sanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
        ?: throw Feil("Fant ikke begrunnelse med apiNavn=${this.sanityApiNavn} for ${this.name} i Sanity.")

fun VedtakBegrunnelseSpesifikasjon.erTilknyttetVilkår(sanityBegrunnelser: List<SanityBegrunnelse>) =
    !this.tilSanityBegrunnelse(sanityBegrunnelser).vilkaar.isNullOrEmpty()

fun SanityBegrunnelse.tilTriggesAv(): TriggesAv {

    return TriggesAv(
        vilkår = this.vilkaar?.map { it.tilVilkår() }?.toSet(),
        personTyper = this.rolle?.map { it.tilPersonType() }?.toSet()
            ?: when {
                this.inneholderVilkår(SanityVilkår.BOSATT_I_RIKET) -> setOf(PersonType.BARN, PersonType.SØKER)
                this.inneholderVilkår(SanityVilkår.LOVLIG_OPPHOLD) -> setOf(PersonType.BARN, PersonType.SØKER)
                this.inneholderVilkår(SanityVilkår.GIFT_PARTNERSKAP) -> setOf(PersonType.BARN)
                this.inneholderVilkår(SanityVilkår.UNDER_18_ÅR) -> setOf(PersonType.BARN)
                this.inneholderVilkår(SanityVilkår.BOR_MED_SOKER) -> setOf(PersonType.BARN)
                else -> setOf(PersonType.BARN, PersonType.SØKER)
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
        valgbar = this.apiNavn != null,
    )
}

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
        if (periode.fom == TIDENES_MORGEN) throw Feil("Prøver å finne fom-dato for begrunnelse, men fikk \"TIDENES_MORGEN\".")
        else periode.fom.forrigeMåned().tilMånedÅr()
}

fun VedtakBegrunnelseSpesifikasjon.hentHjemlerFraSanity(sanityBegrunnelser: List<SanityBegrunnelse>): List<String> {
    return this.tilSanityBegrunnelse(sanityBegrunnelser).hjemler
}

@Converter
class VedtakBegrunnelseSpesifikasjonListConverter : AttributeConverter<List<VedtakBegrunnelseSpesifikasjon>, String> {

    override fun convertToDatabaseColumn(vedtakBegrunnelseSpesifikasjoner: List<VedtakBegrunnelseSpesifikasjon>) =
        konverterEnumsTilString(vedtakBegrunnelseSpesifikasjoner)

    override fun convertToEntityAttribute(string: String?): List<VedtakBegrunnelseSpesifikasjon> =
        konverterStringTilEnums(string)
}
