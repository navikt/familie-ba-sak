package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.persistence.AttributeConverter
import javax.persistence.Converter

interface IVedtakBegrunnelse {

    val triggesAv: TriggesAv
    val vedtakBegrunnelseType: VedtakBegrunnelseType
    fun hentHjemler(): SortedSet<Int>
    fun hentSanityApiNavn(): String
    fun hentBeskrivelse(
            gjelderSøker: Boolean = false,
            barnasFødselsdatoer: List<LocalDate> = emptyList(),
            månedOgÅrBegrunnelsenGjelderFor: String = "",
            målform: Målform
    ): String
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

enum class VedtakBegrunnelseSpesifikasjon(val tittel: String, val erTilgjengeligFrontend: Boolean = true) : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET("Norsk, nordisk bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)
        override fun hentSanityApiNavn() = "innvilgelseNorskNordiskBosattINorge"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "er bosatt i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "er busett i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD("Tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)
        override fun hentSanityApiNavn() = "innvilgelseTredjelandsborgerMedLovligOpphold"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "er bosatt i Norge og har oppholdstillatelse fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "er busett i Noreg og har opphaldsløyve frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseTredjelandsborgerBosattForLovligOpphold"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "har oppholdstillatelse fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "har opphaldsløyve frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseEosborgerSokerHarOppholdsrett"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), personTyper = setOf(PersonType.SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har oppholdsrett som EØS-borger fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi du har opphaldsrett som EØS-borgar frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING("EØS-borger: Skjønnsmessig vurdering av oppholdsrett.") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseEosborgerSkjonnsmessigVurdering"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD),
                                           personTyper = setOf(PersonType.SØKER),
                                           vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du har oppholdsrett som EØS-borger fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du har opphaldsrett som EØS-borgar frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER("Skjønnsmessig vurdering tålt opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseSkjonnsmessigVurderingTaaltOpphold"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD),
                                           vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at ${
                duOgEllerBarnetBarnaFormulering(gjelderSøker,
                                                barnasFødselsdatoer).trim()
            } har oppholdsrett fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at ${
                duOgEllerBarnetBarnaFormulering(gjelderSøker,
                                                barnasFødselsdatoer).trim()
            }  har opphaldsrett frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_OMSORG_FOR_BARN("Adopsjon, surrogati: Omsorgen for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseAdopsjonSurrogatiOmsorgenForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi du har omsorgen for ${barnasFødselsdatoer.barnetBarnaFormulering()} fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Du får barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fordi du har omsorga for ${barnasFødselsdatoer.barnetBarnaFormulering()} frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    INNVILGET_BOR_HOS_SØKER("Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseBarnHarFlyttetTilSoker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "${innvilgetFormulering(gjelderSøker, barnasFødselsdatoer, målform)}" +
                          "bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_BOR_HOS_SØKER_SKJØNNSMESSIG("Skjønnsmessig vurdering - Barn har flyttet til søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseForeldreneBorSammenEndretMottaker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()}" +
                          " fordi vi har kommet fram til at ${barnasFødselsdatoer.barnetBarnaDineDittFormulering()}" +
                          " bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()}" +
                          " fordi vi har kome fram til at ${barnasFødselsdatoer.barnetBarnaDineDittFormulering()}" +
                          " bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseSkjonnsmessigVurderingBarnHarFlyttetTilSoker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()}" +
                          " fordi vi har kommet fram til at du har fått fast omsorg for ${barnasFødselsdatoer.barnetBarnaFormulering()} fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()}" +
                          " fordi vi har kome fram til at du har fått fast omsorg for ${barnasFødselsdatoer.barnetBarnaFormulering()} frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_NYFØDT_BARN_FØRSTE("Nyfødt barn - første barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseNyfodtBarnHarBarnFraFor"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har fått barn og barnet bor sammen med deg. Du får barnetrygd fra måneden etter at barnet er født."
            Målform.NN -> "Du får barnetrygd fordi du har fått barn og barnet bur saman med deg. Du får barnetrygd frå månaden etter at barnet er fødd."
        }
    },
    INNVILGET_NYFØDT_BARN("Nyfødt barn - har barn fra før") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseNyfodtBarnForsteBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får mer barnetrygd fordi du har fått nytt barn, og barna bor sammen med deg. Du får barnetrygden fra måneden etter at det nye barnet ble født."
            Målform.NN -> "Du får meir barnetrygd fordi du har fått nytt barn, og barna bur saman med deg. Du får barnetrygden frå månaden etter at det nye barnet er fødd."
        }
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE("Nyfødt barn - første barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11, 14)
        override fun hentSanityApiNavn() = "innvilgelseNyfodtBarnHarBarnFraFor"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har fått barn og barnet bor sammen med deg."
            Målform.NN -> "Du får barnetrygd fordi du har fått barn og barnet bur saman med deg."
        }
    },
    INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN("Nyfødt barn - har barn fra før") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11, 14)
        override fun hentSanityApiNavn() = "innvilgelseFodselshendelseNyfodtBarnForsteBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får mer barnetrygd fordi du har fått nytt barn, og barna bor sammen med deg."
            Målform.NN -> "Du får meir barnetrygd fordi du har fått nytt barn, og barna bur saman med deg."
        }
    },

    INNVILGET_MEDLEM_I_FOLKETRYGDEN("Medlem i Folketrygden") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseMedlemIFolketrygden"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), medlemskap = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du er medlem i folketrygden fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi du er medlem i folketrygda frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "innvilgelseSokerHarFastOmsorgForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "${
                innvilgetFormulering(gjelderSøker,
                                     barnasFødselsdatoer,
                                     målform)
            } bor sammen med deg. Du får barnetrygd fra samme tidspunkt som barnetrygden til den andre forelderen opphører."
            Målform.NN -> "${
                innvilgetFormulering(gjelderSøker,
                                     barnasFødselsdatoer,
                                     målform)
            } bur saman med deg. Du får barnetrygd frå same tidspunkt som barnetrygda til den andre forelderen opphøyrer. "
        }
    },
    REDUKSJON_BOSATT_I_RIKTET("Barn har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "reduksjonBosattIRiket"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), personTyper = setOf(PersonType.BARN))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN("Barn har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentSanityApiNavn() = "reduksjonLovligOppholdOppholdstillatelseBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), personTyper = setOf(PersonType.BARN))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_FLYTTET_BARN("Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "reduksjonFlyttetBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_BARN_DØD(tittel = "Barn død") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "reduksjonBarnDod"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden er redusert fra måneden etter at barn født ${barnasFødselsdatoer.tilBrevTekst()} døde."
                    Målform.NN -> "Barnetrygda er redusert frå månaden etter at barn fødd ${barnasFødselsdatoer.tilBrevTekst()} døydde."
                }
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN("Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "reduksjonFastOmsorgForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi vi har kommet fram til at barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi vi har kome fram til at barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_MANGLENDE_OPPLYSNINGER(tittel = "Ikke mottatt opplysninger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override fun hentSanityApiNavn() = "reduksjonManglendeOpplysninger"
        override val triggesAv = TriggesAv(personerManglerOpplysninger = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi du ikke har sendt oss de opplysningene vi ba om."
                    Målform.NN -> "Barnetrygda er redusert fordi du ikkje har sendt oss dei opplysningane vi ba om."
                }
    },
    REDUKSJON_UNDER_18_ÅR("Under 18") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "reduksjonUnder18Aar"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String {
            val fødselsMånedOgÅrForAlder18 = YearMonth.from(LocalDate.now()).minusYears(18)
            val fødselsdatoerForBarn18År = barnasFødselsdatoer.filter { it.toYearMonth().equals(fødselsMånedOgÅrForAlder18) }
            return when (målform) {
                Målform.NB -> "Barnetrygden reduseres fordi barn født ${fødselsdatoerForBarn18År.tilBrevTekst()} er 18 år."
                Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${fødselsdatoerForBarn18År.tilBrevTekst()} er 18 år."
            }
        }
    },
    REDUKSJON_UNDER_6_ÅR("Barn 6 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(10)
        override fun hentSanityApiNavn() = "reduksjonUnder6Aar"
        override val triggesAv = TriggesAv(barnMedSeksårsdag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String {
            val fødselsMånedOgÅrForAlder6 = YearMonth.from(LocalDate.now()).minusYears(6)
            val fødselsdatoerForBarn6År = barnasFødselsdatoer.filter { it.toYearMonth().equals(fødselsMånedOgÅrForAlder6) }
            return when (målform) {
                Målform.NB -> "Barnetrygden reduseres fordi barn født ${fødselsdatoerForBarn6År.tilBrevTekst()} er 6 år."
                Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${fødselsdatoerForBarn6År.tilBrevTekst()} er 6 år."
            }
        }
    },
    REDUKSJON_DELT_BOSTED_ENIGHET("Enighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "reduksjonDeltBostedEnighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi avtalen om delt bosted for barn født ${barnasFødselsdatoer.tilBrevTekst()} er opphørt fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi avtalen om delt bustad for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} er opphøyrt frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_DELT_BOSTED_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "reduksjonDeltBostedUenighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du og den andre forelderen er uenige om avtalen om delt bosted. Vi har kommet fram til at avtalen om delt bosted for barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger praktiseres fra $månedOgÅrBegrunnelsenGjelderFor." +
                                  "\nVed uenighet mellom foreldrene om avtalen om delt bosted, kan barnetrygden opphøres fra måneden etter at vi fikk søknad om full barnetrygd."
                    Målform.NN -> "Du og den andre forelderen er usamde om avtalen om delt bustad. Vi har kome fram til at avtalen om delt bustad for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger blir praktisert frå $månedOgÅrBegrunnelsenGjelderFor." +
                                  "\nNår de er usamde om avtalen om delt bustad, kan vi opphøyre barnetrygda til deg frå og med månaden etter at vi fekk søknad om full barnetrygd. "
                }
    },
    REDUKSJON_ENDRET_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 12)
        override fun hentSanityApiNavn() = "reduksjonEndretMottaker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi den andre forelderen har søkt om barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()}."
                    Målform.NN -> "Barnetrygda er redusert fordi den andre forelderen har søkt om barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()}."
                }
    },
    REDUKSJON_FRITEKST("Fritekst", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf()
        override fun hentSanityApiNavn() = TODO()
        override val triggesAv = TriggesAv(valgbar = false)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = ""
    },
    INNVILGET_SATSENDRING("Satsendring") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE

        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 10)
        override fun hentSanityApiNavn() = "innvilgelseAutotekstVedSatsendring"
        override val triggesAv = TriggesAv(satsendring = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden endres fordi det har vært en satsendring."
                    Målform.NN -> "Barnetrygda er endra fordi det har vore ei satsendring."
                }
    },
    AVSLAG_BOSATT_I_RIKET("Ikke bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagBosattIRiket"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi ${
                        duOgEllerBarnetBarnaFormulering(gjelderSøker, barnasFødselsdatoer)
                                .trim()
                    } ikke er bosatt i Norge${fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)}."
                    Målform.NN -> "Barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fordi${
                        duOgEllerBarnetBarnaFormulering(gjelderSøker, barnasFødselsdatoer)
                                .trim()
                    } ikkje er busett i Noreg${fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)}."
                }
    },
    AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER("Tredjelandsborger uten lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagLovligOppholdTredjelandsborger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi ${
                        duOgEllerBarnetBarnaFormulering(gjelderSøker, barnasFødselsdatoer).trim()
                    } ikke har oppholdstillatelse i Norge${fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)}."
                    Målform.NN -> "Barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fordi ${
                        duOgEllerBarnetBarnaFormulering(gjelderSøker, barnasFødselsdatoer).trim()
                    } ikkje har opphaldsløyve i Noreg${fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)}."
                }
    },
    AVSLAG_BOR_HOS_SØKER("Barn bor ikke med søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagBorHosSoker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi ${barnasFødselsdatoer.barnetBarnaFormulering()} ikke bor hos deg${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fordi ${barnasFødselsdatoer.barnetBarnaFormulering()} ikkje bur hos deg${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_OMSORG_FOR_BARN("Adopsjon, surrogati, beredskapshjem, vurdering av fast bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagOmsorgForBarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi vi har kommet fram til at du ikke har fast omsorg for ${barnasFødselsdatoer.barnetBarnaFormulering()}${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fordi vi har kome fram til at du ikkje har fast omsorg for ${barnasFødselsdatoer.barnetBarnaFormulering()}${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger uten oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagLovligOppholdEosBorger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi vi har kommet fram til at${
                        duOgEllerBarnaFødtFormulering(gjelderSøker, barnasFødselsdatoer, målform)
                    }ikke har oppholdsrett som EØS-borger${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd fordi vi har kome fram til at${
                        duOgEllerBarnaFødtFormulering(gjelderSøker,
                                                      barnasFødselsdatoer,
                                                      målform)
                    }ikkje har opphaldsrett som EØS-borgar${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER("Skjønnsmessig vurdering opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagLovligOppholdSkjonnsmessigVurderingTredjelandsborger"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi vi har kommet fram til at${
                        duOgEllerBarnaFødtFormulering(gjelderSøker,
                                                      barnasFødselsdatoer,
                                                      målform)
                    }ikke har oppholdsrett i Norge${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd fordi vi har komme fram til at${
                        duOgEllerBarnaFødtFormulering(gjelderSøker,
                                                      barnasFødselsdatoer,
                                                      målform)
                    }ikkje har opphaldsrett i Noreg${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_MEDLEM_I_FOLKETRYGDEN("Unntatt medlemskap i Folketrygden") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagMedlemIFolketrygden"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), medlemskap = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi${
                        duOgEllerBarnaFødtFormulering(gjelderSøker,
                                                      barnasFødselsdatoer,
                                                      målform)
                    }ikke er medlem av folketrygden${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd fordi${
                        duOgEllerBarnaFødtFormulering(gjelderSøker,
                                                      barnasFødselsdatoer,
                                                      målform)
                    }ikkje er medlem av folketrygda${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_FORELDRENE_BOR_SAMMEN("Foreldrene bor sammen", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 12)
        override fun hentSanityApiNavn() = "avslagForeldreneBorSammen"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi den andre forelderen allerede får barnetrygd for ${barnasFødselsdatoer.barnetBarnaFormulering()}."
                    Målform.NN -> "Barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fordi den andre forelderen allereie mottek barnetrygd for ${barnasFødselsdatoer.barnetBarnaFormulering()}."
                }
    },
    AVSLAG_UNDER_18_ÅR("Barn over 18 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(11)
        override fun hentSanityApiNavn() = "avslagUnder18Aar"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi ${barnasFødselsdatoer.barnetBarnaFormulering()} er over 18 år. "
                    Målform.NN -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi ${barnasFødselsdatoer.barnetBarnaFormulering()} er over 18 år."
                }
    },
    AVSLAG_UGYLDIG_AVTALE_OM_DELT_BOSTED("Ugyldig avtale om delt bosted", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2)
        override fun hentSanityApiNavn() = "avslagUgyldigAvtaleOmDeltBosted "
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi du ikke har en gyldig avtale om delt bosted for ${barnasFødselsdatoer.barnetBarnaFormulering()}${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }. Barnetrygden kan derfor ikke deles. "
                    Målform.NN -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi du ikkje har ein gyldig avtale om delt bustad for ${barnasFødselsdatoer.barnetBarnaFormulering()}${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }. Barnetrygda kan derfor ikkje delast."
                }
    },
    AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED("Ikke avtale om delt bosted", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2)
        override fun hentSanityApiNavn() = "avslagIkkeAvtaleOmDeltBosted"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi du ikke har en avtale om delt bosted for ${barnasFødselsdatoer.barnetBarnaFormulering()}${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }. Barnetrygden kan derfor ikke deles. "
                    Målform.NN -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi du ikkje har ein avtale om delt bustad for ${barnasFødselsdatoer.barnetBarnaFormulering()}${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }. Barnetrygda kan derfor ikkje delast."
                }
    },
    AVSLAG_OPPLYSNINGSPLIKT("Ikke mottatt opplysninger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override fun hentSanityApiNavn() = "avslagOpplysningsplikt"
        override val triggesAv = TriggesAv(personerManglerOpplysninger = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi du ikke har sendt oss de opplysningene vi ba om. "
                    Målform.NN -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi du ikkje har sendt oss dei opplysningane vi ba om."
                }
    },
    AVSLAG_SÆRKULLSBARN("Ektefelle eller samboers særkullsbarn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2)
        override fun hentSanityApiNavn() = "avslagSaerkullsbarn"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi ${barnasFødselsdatoer.barnetBarnaFormulering()} bor sammen med en av foreldrene sine. I slike tilfeller er det forelderen til ${barnasFødselsdatoer.barnetBarnaFormulering()} som har rett til barnetrygden."
                    Målform.NN -> "Barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fordi ${barnasFødselsdatoer.barnetBarnaFormulering()} bur saman med ein av foreldra sine. I slike tilfelle er det forelderen til ${barnasFødselsdatoer.barnetBarnaFormulering()} som har rett til barnetrygda."
                }
    },
    AVSLAG_UREGISTRERT_BARN("Barn uten fødselsnummer", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentSanityApiNavn() = "avslagUregistrertBarn"
        override val triggesAv = TriggesAv(valgbar = false)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi vi har kommet fram til at det ikke er bekreftet at ${barnasFødselsdatoer.barnetBarnaFormulering()} er bosatt i Norge."
                    Målform.NN -> "Barnetrygd  fordi vi har komme fram til at det ikkje er stadfesta at ${barnasFødselsdatoer.barnetBarnaFormulering()} er busett i Noreg."
                }
    },
    AVSLAG_FRITEKST("Fritekst", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf()
        override fun hentSanityApiNavn() = throw Feil("Skal ikke hente fritekst for avslag fra sanity.")
        override val triggesAv = TriggesAv(valgbar = false)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = ""
    },
    OPPHØR_BARN_FLYTTET_FRA_SØKER("Barn bor ikke med søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "opphorBarnBorIkkeMedSoker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_UTVANDRET("Barn og/eller søker har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "opphorFlyttetFraNorge"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when {
                    !gjelderSøker -> {
                        when (målform) {
                            Målform.NB -> "Barn født ${barnasFødselsdatoer.tilBrevTekst()} har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                            Målform.NN -> "Barn fødd ${barnasFødselsdatoer.tilBrevTekst()} har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                        }
                    }
                    barnasFødselsdatoer.isEmpty() -> {
                        when (målform) {
                            Målform.NB -> "Du har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                            Målform.NN -> "Du har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                        }
                    }
                    else -> {
                        when (målform) {
                            Målform.NB -> "Du og barn født ${barnasFødselsdatoer.tilBrevTekst()} har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                            Målform.NN -> "Du og barn fødd ${barnasFødselsdatoer.tilBrevTekst()} har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                        }
                    }
                }
    },
    OPPHØR_BARN_DØD(tittel = "Et barn er dødt") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "opphorEtBarnErDodt"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnet ditt som er født ${barnasFødselsdatoer.tilBrevTekst()} døde. Barnetrygden opphører fra måneden etter at barnet døde."
                    Målform.NN -> "Barnet ditt som er fødd ${barnasFødselsdatoer.tilBrevTekst()} døydde. Barnetrygda opphøyrer frå månaden etter at barnet døydde."
                }
    },
    OPPHØR_FLERE_BARN_DØD(tittel = "Flere barn er døde") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "opphorFlereBarnErDode"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barna dine som er født ${barnasFødselsdatoer.tilBrevTekst()} døde. Barnetrygden opphører fra måneden etter at ${barnasFødselsdatoer.barnetBarnaFormulering()} døde."
                    Målform.NN -> "barna dine som er fødd ${barnasFødselsdatoer.tilBrevTekst()} døydde. Barnetrygda opphøyrer frå månaden etter at ${barnasFødselsdatoer.barnetBarnaFormulering()} døydde."
                }
    },
    OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG("Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "opphorSokerHarIkkeFastOmsorg"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Vi har kommet fram til at barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Vi har kome fram til at barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_HAR_IKKE_OPPHOLDSTILLATELSE("Barn og/eller søker har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentSanityApiNavn() = "opphorHarIkkeOppholdstillatelse"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when {
                    !gjelderSøker -> {
                        when (målform) {
                            Målform.NB -> "Barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                            Målform.NN -> "Barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                        }
                    }
                    barnasFødselsdatoer.isEmpty() -> {
                        when (målform) {
                            Målform.NB -> "Du ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                            Målform.NN -> "Du ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                        }
                    }
                    else -> {
                        when (målform) {
                            Målform.NB -> "Du og barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                            Målform.NN -> "Du og barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                        }
                    }
                }
    },
    OPPHØR_IKKE_MOTTATT_OPPLYSNINGER(tittel = "Ikke mottatt opplysninger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override fun hentSanityApiNavn() = "opphorIkkeMottattOpplysninger"
        override val triggesAv = TriggesAv(personerManglerOpplysninger = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du ikke har sendt oss de opplysningene vi ba om."
                    Målform.NN -> "Du ikkje har sendt oss dei opplysningane vi ba om."
                }
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_ENIGHET("Enighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "opphorDeltBostedOpphortEnighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Avtalen om delt bosted for barn født ${barnasFødselsdatoer.tilBrevTekst()}  er opphørt fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Avtalen om delt bustad for barn fødd ${barnasFødselsdatoer.tilBrevTekst()}  er opphøyrt frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "opphorDeltBostedOpphortUenighet"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), deltbosted = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du og den andre forelderen er uenige om avtalen om delt bosted. Vi har kommet fram til at avtale om delt bosted for barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger praktiseres fra $månedOgÅrBegrunnelsenGjelderFor.\r" +
                                  "Ved uenighet mellom foreldrene om avtalen om delt bosted, kan barnetrygden opphøres fra måneden etter at vi fikk søknad om full barnetrygd."
                    Målform.NN -> "Du og den andre forelderen er usamde om avtalen om delt bustad. Vi har kome fram til at avtalen om delt bustad for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger blir praktisert frå $månedOgÅrBegrunnelsenGjelderFor.\r" +
                                  "Når de er usamde om avtalen om delt bustad, kan vi opphøyre barnetrygda til deg frå og med månaden etter at vi fekk søknad om full barnetrygd."
                }
    },
    OPPHØR_UNDER_18_ÅR("Barn 18 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentSanityApiNavn() = "opphorUnder18Aar"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UNDER_18_ÅR))

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født ${barnasFødselsdatoer.tilBrevTekst()} er 18 år."
                    Målform.NN -> "Barn fødd ${barnasFødselsdatoer.tilBrevTekst()} er 18 år. "
                }
    },
    OPPHØR_FRITEKST("Fritekst", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf()
        override fun hentSanityApiNavn() = throw Feil("Fritekst for opphør skal ikke hentes fra Sanity.")
        override val triggesAv = TriggesAv(valgbar = false)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = ""
    },
    OPPHØR_ENDRET_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 12)
        override fun hentSanityApiNavn() = "opphorEndretMottaker"
        override val triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), vurderingAnnetGrunnlag = true)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Den andre forelderen har søkt om barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()}."
                    Målform.NN -> "Den andre forelderen har søkt om barnetrygd for barn fødd ${barnasFødselsdatoer.tilBrevTekst()}."
                }
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET("Søker og barn oppholder seg i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetSokerOgBarnBosattIRiket"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du og ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt er bosatt i Norge."
                    Målform.NN -> "Du får barnetrygd fordi du og ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt er busett i Noreg."
                }
    },
    FORTSATT_INNVILGET_SØKER_BOSATT_I_RIKET("Søker oppholder seg i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetSokerBosattIRiket"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du fortsatt er bosatt i Norge."
                    Målform.NN -> "Du får barnetrygd fordi du fortsatt er busett i Noreg."
                }
    },
    FORTSATT_INNVILGET_BARN_BOSATT_I_RIKET("Barn oppholder seg i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetBarnBosattIRiket"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt er bosatt i Norge."
                    Målform.NN -> "Du får barnetrygd fordi ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt er busett i Noreg."
                }
    },
    FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger søker og barn fortsatt lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetBarnOgSokerLovligOppholdOppholdstillatelse"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du og ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt har oppholdstillatelse."
                    Målform.NN -> "Du får barnetrygd fordi du og ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt har opphaldsløyve."
                }
    },
    FORTSATT_INNVILGET_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger søker fortsatt lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetSokerLovligOppholdOppholdstillatelse"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du fortsatt har oppholdstillatelse."
                    Målform.NN -> "Du får barnetrygd fordi du fortsatt har opphaldsløyve."
                }
    },
    FORTSATT_INNVILGET_BARN_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger barn fortsatt lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetBarnLovligOppholdOppholdstillatelse"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt har oppholdstillatelse."
                    Målform.NN -> "Du får barnetrygd fordi ${
                        barnasFødselsdatoer.barnetBarnaFormulering()
                    } fortsatt har opphaldsløyve."
                }
    },
    FORTSATT_INNVILGET_BOR_MED_SØKER("Barn bosatt med søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetBorMedSoker"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi${
                        duOgEllerBarnaFødtFormulering(gjelderSøker,
                                                      barnasFødselsdatoer,
                                                      målform)
                    }fortsatt bor hos deg."
                    Målform.NN -> "Du får barnetrygd fordi${
                        duOgEllerBarnaFødtFormulering(gjelderSøker,
                                                      barnasFødselsdatoer,
                                                      målform)
                    }fortsatt bur hos deg."
                }
    },
    FORTSATT_INNVILGET_FAST_OMSORG("Fortsatt fast omsorg for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetFastOmsorg"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du fortsatt har fast omsorg for ${if (barnasFødselsdatoer.size == 1) "barnet" else "barna"}."
                    Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du fortsatt har fast omsorg for ${if (barnasFødselsdatoer.size == 1) "barnet" else "barna"}."
                }
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_EØS("EØS-borger: Søker har oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetLovligOppholdEOS"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du fortsatt har oppholdsrett som EØS-borger."
                    Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du fortsatt har opphaldsrett som EØS-borgar."
                }
    },
    FORTSATT_INNVILGET_LOVLIG_OPPHOLD_TREDJELANDSBORGER("Tålt opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetLovligOppholdTredjelandsborger"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du fortsatt har oppholdsrett."
                    Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du fortsatt har opphaldsrett."
                }
    },
    FORTSATT_INNVILGET_UENDRET_TRYGD("Har barnetrygden det er søkt om") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentSanityApiNavn() = "fortsattInnvilgetUendretTrygd"
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du får uendret barnetrygd fordi du allerede mottar barnetrygden som du har søkt om."
                    Målform.NN -> "Du får uendra barnetrygd fordi du allereie mottek barnetrygda som du har søkt om."
                }
    },
    FORTSATT_INNVILGET_FRITEKST("Fortsatt innvilget fritekst", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf()
        override fun hentSanityApiNavn() = TODO()
        override val triggesAv = TriggesAv()

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = ""
    };

    fun erFritekstBegrunnelse() = listOf(REDUKSJON_FRITEKST,
                                         OPPHØR_FRITEKST,
                                         AVSLAG_FRITEKST,
                                         FORTSATT_INNVILGET_FRITEKST).contains(this)

    fun triggesForPeriode(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
                          vilkårsvurdering: Vilkårsvurdering,
                          persongrunnlag: PersonopplysningGrunnlag,
                          identerMedUtbetaling: List<String>): Boolean {
        if (!this.triggesAv.valgbar) return false

        if (vedtaksperiodeMedBegrunnelser.type != this.vedtakBegrunnelseType.tilVedtaksperiodeType()) return false

        if (this.triggesAv.personerManglerOpplysninger) return vilkårsvurdering.harPersonerManglerOpplysninger()

        if (this.triggesAv.barnMedSeksårsdag) return persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom)

        if (this.triggesAv.satsendring)
            return SatsService.finnSatsendring(vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN).isNotEmpty()

        return hentPersonerForAlleUtgjørendeVilkår(
                vilkårsvurdering = vilkårsvurdering,
                vedtaksperiode = Periode(
                        fom = vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                        tom = vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
                ),
                oppdatertBegrunnelseType = this.vedtakBegrunnelseType,
                utgjørendeVilkår = this.triggesAv.vilkår,
                aktuellePersonerForVedtaksperiode = persongrunnlag.personer
                        .filter { person -> this.triggesAv.personTyper.contains(person.type) }
                        .filter { person ->
                            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE) {
                                identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
                            } else true
                        },
                deltBosted = this.triggesAv.deltbosted,
                vurderingAnnetGrunnlag = this.triggesAv.vurderingAnnetGrunnlag
        ).isNotEmpty()
    }

    companion object {

        fun List<LocalDate>.tilBrevTekst(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })
        fun List<LocalDate>.barnetBarnaFormulering(): String = if (this.size > 1) "barna" else if (this.size == 1) "barnet" else ""
        fun List<LocalDate>.barnetBarnaDineDittFormulering(): String = if (this.size > 1) "barna dine" else if (this.size == 1) "barnet ditt" else ""

        fun duOgEllerBarnaFødtFormulering(gjelderSøker: Boolean, barnasFødselsdatoer: List<LocalDate>, målform: Målform): String {
            val duFormulering =
                    if (gjelderSøker && barnasFødselsdatoer.isNotEmpty()) " du og " else if (gjelderSøker) " du " else " "
            return when (målform) {
                Målform.NB -> duFormulering + if (barnasFødselsdatoer.isNotEmpty()) "barn født ${barnasFødselsdatoer.tilBrevTekst()} " else ""
                Målform.NN -> duFormulering + if (barnasFødselsdatoer.isNotEmpty()) "barn fødd ${barnasFødselsdatoer.tilBrevTekst()} " else ""
            }
        }

        fun duOgEllerBarnetBarnaFormulering(gjelderSøker: Boolean, barnasFødselsdatoer: List<LocalDate>) =
                "${if (gjelderSøker && barnasFødselsdatoer.isNotEmpty()) " du og " else if (gjelderSøker) " du " else " "}${barnasFødselsdatoer.barnetBarnaFormulering()}"

        fun innvilgetFormulering(gjelderSøker: Boolean, barnasFødselsdatoer: List<LocalDate>, målform: Målform) =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd${
                        if (barnasFødselsdatoer.isNotEmpty()) " for barn født ${barnasFødselsdatoer.tilBrevTekst()} " else " "
                    }fordi ${
                        duOgEllerBarnetBarnaFormulering(gjelderSøker,
                                                        barnasFødselsdatoer).trim()
                    } "
                    Målform.NN -> "Du får barnetrygd${
                        if (barnasFødselsdatoer.isNotEmpty()) " for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} " else " "
                    }fordi ${
                        duOgEllerBarnetBarnaFormulering(gjelderSøker,
                                                        barnasFødselsdatoer).trim()
                    } "
                }

        fun fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor: String, målform: Målform) =
                when (målform) {
                    Målform.NB -> if (månedOgÅrBegrunnelsenGjelderFor.isNotBlank()) " fra $månedOgÅrBegrunnelsenGjelderFor" else ""
                    Målform.NN -> if (månedOgÅrBegrunnelsenGjelderFor.isNotBlank()) " frå $månedOgÅrBegrunnelsenGjelderFor" else ""
                }
    }
}

val hjemlerTilhørendeFritekst = setOf(2, 4, 11)

val vedtakBegrunnelserIkkeTilknyttetVilkår = VedtakBegrunnelseSpesifikasjon.values().filter { it.triggesAv.vilkår == null }

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
    if (this.erFritekstBegrunnelse()) {
        throw Feil("Kan ikke fastsette fritekstbegrunnelse på begrunnelser på vedtaksperioder. Bruk heller fritekster.")
    }

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

@Converter
class VedtakBegrunnelseSpesifikasjonListConverter : AttributeConverter<List<VedtakBegrunnelseSpesifikasjon>, String> {

    override fun convertToDatabaseColumn(vedtakBegrunnelseSpesifikasjonList: List<VedtakBegrunnelseSpesifikasjon>): String =
            vedtakBegrunnelseSpesifikasjonList.joinToString(separator = SPLIT_CHAR)

    override fun convertToEntityAttribute(string: String?): List<VedtakBegrunnelseSpesifikasjon> =
            if (string.isNullOrBlank()) emptyList() else string.split(SPLIT_CHAR)
                    .map { VedtakBegrunnelseSpesifikasjon.valueOf(it) }

    companion object {

        private const val SPLIT_CHAR = ";"
    }
}
