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
import java.util.SortedSet
import javax.persistence.AttributeConverter
import javax.persistence.Converter

interface IVedtakBegrunnelse {

    val sanityApiNavn: String
    val vedtakBegrunnelseType: VedtakBegrunnelseType

    @Deprecated("Skal hentes fra Sanity, se SanityBegrunnelse.tilTriggesAv()")
    val triggesAv: TriggesAv

    @Deprecated("Skal hentes fra Sanity")
    fun hentHjemler(): SortedSet<Int>
}

data class TriggesAv(val vilkår: Set<Vilkår>? = null,
                     val personTyper: Set<PersonType> = setOf(PersonType.BARN, PersonType.SØKER),
                     val personerManglerOpplysninger: Boolean = false,
                     val satsendring: Boolean = false,
                     val barnMedSeksårsdag: Boolean = false,
                     val vurderingAnnetGrunnlag: Boolean = false,
                     val medlemskap: Boolean = false,
                     val deltbosted: Boolean = false,
                     val valgbar: Boolean = true)

enum class VedtakBegrunnelseSpesifikasjon(
        @Deprecated("Skal hentes fra sanity")
        val tittel: String,

        val erTilgjengeligFrontend: Boolean = true,
) : IVedtakBegrunnelse {

    INNVILGET_BOSATT_I_RIKTET("Norsk, nordisk bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)
        override val sanityApiNavn = "innvilgetBosattIRiket"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))
    },
    INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD("Tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)
        override val sanityApiNavn = "innvilgetBosattIRiketLovligOpphold"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD))
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetLovligOppholdOppholdstillatelse"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), personTyper = setOf(PersonType.SØKER))
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING("EØS-borger: Skjønnsmessig vurdering av oppholdsrett.") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetLovligOppholdEOSBorgerSkjonnsmessigVurdering"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD),
                                           personTyper = setOf(PersonType.SØKER),
                                           vurderingAnnetGrunnlag = true)
    },
    INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER("Skjønnsmessig vurdering tålt opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD),
                                           vurderingAnnetGrunnlag = true)
    },
    INNVILGET_OMSORG_FOR_BARN("Adopsjon, surrogati: Omsorgen for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetOmsorgForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    INNVILGET_BOR_HOS_SØKER("Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetBorHosSoker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    INNVILGET_BOR_HOS_SØKER_SKJØNNSMESSIG("Skjønnsmessig vurdering - Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetBorHosSokerSkjonnsmessig"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    INNVILGET_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetFastOmsorgForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    INNVILGET_NYFØDT_BARN_FØRSTE("Nyfødt barn - første barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetNyfodtBarnForste"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))
    },
    INNVILGET_NYFØDT_BARN("Nyfødt barn - har barn fra før") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetNyfodtBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE("Nyfødt barn - første barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11, 14)
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarnForste"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN("Nyfødt barn - har barn fra før") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11, 14)
        override val sanityApiNavn = "innvilgetFodselshendelseNyfodtBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))
    },

    INNVILGET_MEDLEM_I_FOLKETRYGDEN("Medlem i Folketrygden") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetMedlemIFolketrygden"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), medlemskap = true)
    },
    INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "innvilgetBarnBorSammenMedMottaker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

    },
    REDUKSJON_BOSATT_I_RIKTET("Barn har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "reduksjonBosattIRiket"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), personTyper = setOf(PersonType.BARN))
    },
    REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN("Barn har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override val sanityApiNavn = "reduksjonLovligOppholdOppholdstillatelseBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), personTyper = setOf(PersonType.BARN))
    },
    REDUKSJON_FLYTTET_BARN("Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "reduksjonFlyttetBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    REDUKSJON_BARN_DØD("Barn død") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "reduksjonBarnDod"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN("Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "reduksjonFastOmsorgForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    REDUKSJON_MANGLENDE_OPPLYSNINGER("Ikke mottatt opplysninger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override val sanityApiNavn = "reduksjonManglendeOpplysninger"
        override val triggesAv = TriggesAv(personerManglerOpplysninger = true)
    },
    REDUKSJON_UNDER_18_ÅR("Under 18") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "reduksjonUnder18Aar"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))
    },
    REDUKSJON_UNDER_6_ÅR("Barn 6 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(10)
        override val sanityApiNavn = "reduksjonUnder6Aar"
        override val triggesAv = TriggesAv(barnMedSeksårsdag = true)
    },
    REDUKSJON_DELT_BOSTED_ENIGHET("Enighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "reduksjonDeltBostedEnighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)
    },
    REDUKSJON_DELT_BOSTED_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "reduksjonDeltBostedUenighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)
    },
    REDUKSJON_ENDRET_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 12)
        override val sanityApiNavn = "reduksjonEndretMottaker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    INNVILGET_SATSENDRING("Satsendring") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE

        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 10)
        override val sanityApiNavn = "innvilgetSatsendring"
        override val triggesAv = TriggesAv(satsendring = true)
    },
    AVSLAG_BOSATT_I_RIKET("Ikke bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagBosattIRiket"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))
    },
    AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER("Tredjelandsborger uten lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagLovligOppholdTredjelandsborger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))
    },
    AVSLAG_BOR_HOS_SØKER("Barn bor ikke med søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagBorHosSoker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    AVSLAG_OMSORG_FOR_BARN("Adopsjon, surrogati, beredskapshjem, vurdering av fast bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagOmsorgForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger uten oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagLovligOppholdEosBorger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))
    },
    AVSLAG_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER("Skjønnsmessig vurdering opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), vurderingAnnetGrunnlag = true)
    },
    AVSLAG_MEDLEM_I_FOLKETRYGDEN("Unntatt medlemskap i Folketrygden") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagMedlemIFolketrygden"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), medlemskap = true)
    },
    AVSLAG_FORELDRENE_BOR_SAMMEN("Foreldrene bor sammen") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 12)
        override val sanityApiNavn = "avslagForeldreneBorSammen"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    AVSLAG_UNDER_18_ÅR("Barn over 18 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(11)
        override val sanityApiNavn = "avslagUnder18Aar"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))
    },
    AVSLAG_UGYLDIG_AVTALE_OM_DELT_BOSTED("Ugyldig avtale om delt bosted", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2)
        override val sanityApiNavn = "avslagUgyldigAvtaleOmDeltBosted"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)
    },
    AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED("Ikke avtale om delt bosted", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2)
        override val sanityApiNavn = "avslagIkkeAvtaleOmDeltBosted"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)
    },
    AVSLAG_OPPLYSNINGSPLIKT("Ikke mottatt opplysninger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override val sanityApiNavn = "avslagOpplysningsplikt"
        override val triggesAv = TriggesAv(personerManglerOpplysninger = true)
    },
    AVSLAG_SÆRKULLSBARN("Ektefelle eller samboers særkullsbarn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2)
        override val sanityApiNavn = "avslagSaerkullsbarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    AVSLAG_UREGISTRERT_BARN("Barn uten fødselsnummer") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override val sanityApiNavn = "avslagUregistrertBarn"
        override val triggesAv = TriggesAv(valgbar = false)
    },
    OPPHØR_BARN_FLYTTET_FRA_SØKER("Barn bor ikke med søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "opphorBarnBorIkkeMedSoker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    OPPHØR_UTVANDRET("Barn og/eller søker har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "opphorFlyttetFraNorge"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))
    },
    OPPHØR_BARN_DØD("Et barn er dødt") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "opphorEtBarnErDodt"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    OPPHØR_FLERE_BARN_DØD("Flere barn er døde") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "opphorFlereBarnErDode"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))
    },
    OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG("Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "opphorSokerHarIkkeFastOmsorg"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE("Barn og/eller søker har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override val sanityApiNavn = "opphorHarIkkeOppholdstillatelse"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))

    },
    OPPHØR_IKKE_MOTTATT_OPPLYSNINGER("Ikke mottatt opplysninger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override val sanityApiNavn = "opphorIkkeMottattOpplysninger"
        override val triggesAv = TriggesAv(personerManglerOpplysninger = true)
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_ENIGHET("Enighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "opphorDeltBostedOpphortEnighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "opphorDeltBostedOpphortUenighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)
    },
    OPPHØR_UNDER_18_ÅR("Barn 18 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override val sanityApiNavn = "opphorUnder18Aar"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))
    },
    OPPHØR_ENDRET_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 12)
        override val sanityApiNavn = "opphorEndretMottaker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET("Søker og barn oppholder seg i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetSokerOgBarnBosattIRiket"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_SØKER_BOSATT_I_RIKET("Søker oppholder seg i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetSokerBosattIRiket"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_BARN_BOSATT_I_RIKET("Barn oppholder seg i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetBarnBosattIRiket"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger søker og barn fortsatt lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetBarnOgSokerLovligOppholdOppholdstillatelse"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger søker fortsatt lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetSokerLovligOppholdOppholdstillatelse"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_BARN_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger barn fortsatt lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetBarnLovligOppholdOppholdstillatelse"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_BOR_MED_SØKER("Barn bosatt med søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetBorMedSoker"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_FAST_OMSORG("Fortsatt fast omsorg for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetFastOmsorg"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_EØS("EØS-borger: Søker har oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetLovligOppholdEOS"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_TREDJELANDSBORGER("Tålt opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetLovligOppholdTredjelandsborger"
        override val triggesAv = TriggesAv()
    },
    FORTSATT_INNVILGET_UENDRET_TRYGD("Har barnetrygden det er søkt om") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override val sanityApiNavn = "fortsattInnvilgetUendretTrygd"
        override val triggesAv = TriggesAv()
    };

    fun triggesForPeriode(
            vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
            vilkårsvurdering: Vilkårsvurdering,
            persongrunnlag: PersonopplysningGrunnlag,
            identerMedUtbetaling: List<String>,
            triggesAv: TriggesAv = this.triggesAv,
            vedtakBegrunnelseType: VedtakBegrunnelseType = this.vedtakBegrunnelseType,
    ): Boolean {
        if (!triggesAv.valgbar) return false

        if (vedtaksperiodeMedBegrunnelser.type != vedtakBegrunnelseType.tilVedtaksperiodeType()) return false

        if (triggesAv.personerManglerOpplysninger) return vilkårsvurdering.harPersonerManglerOpplysninger()

        if (triggesAv.barnMedSeksårsdag) return persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom)

        if (triggesAv.satsendring)
            return SatsService.finnSatsendring(vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN).isNotEmpty()

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

@Deprecated("Bruk VedtakBegrunnelseSpesifikasjon.erTilknyttetVilkår")
val vedtakBegrunnelserIkkeTilknyttetVilkår = VedtakBegrunnelseSpesifikasjon.values().filter { it.triggesAv.vilkår == null }

fun VedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(sanityBegrunnelser: List<SanityBegrunnelse>): SanityBegrunnelse =
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
            vurderingAnnetGrunnlag = (this.inneholderLovligOppholdTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG)
                                      || this.inneholderBosattIRiketTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG)
                                      || this.inneholderGiftPartnerskapTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG)
                                      || this.inneholderBorMedSøkerTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG)
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
        throw Feil("Begrunnelsestype ${this.vedtakBegrunnelseType} passer ikke med " +
                   "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden.")
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

    override fun convertToEntityAttribute(string: String?): List<VedtakBegrunnelseSpesifikasjon> = konverterStringTilEnums(string)
}
