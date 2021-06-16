package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import java.time.LocalDate
import java.util.SortedSet

interface IVedtakBegrunnelse {

    val vedtakBegrunnelseType: VedtakBegrunnelseType
    fun hentHjemler(): SortedSet<Int>
    fun hentBeskrivelse(
            gjelderSøker: Boolean = false,
            barnasFødselsdatoer: List<LocalDate> = emptyList(),
            månedOgÅrBegrunnelsenGjelderFor: String = "",
            målform: Målform
    ): String
}

enum class VedtakBegrunnelseSpesifikasjon(val tittel: String, val erTilgjengeligFrontend: Boolean = true) : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET("Norsk, nordisk bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${
                duOgEllerBarnaFødtFormulering(gjelderSøker,
                                              barnasFødselsdatoer,
                                              målform)
            }er bosatt i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi${
                duOgEllerBarnaFødtFormulering(gjelderSøker,
                                              barnasFødselsdatoer,
                                              målform)
            }er busett i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD("Tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${
                duOgEllerBarnaFødtFormulering(gjelderSøker,
                                              barnasFødselsdatoer,
                                              målform)
            }er bosatt i Norge og har oppholdstillatelse fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi${
                duOgEllerBarnaFødtFormulering(gjelderSøker,
                                              barnasFødselsdatoer,
                                              målform)
            }er busett i Noreg og har opphaldsløyve frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${
                duOgEllerBarnaFødtFormulering(gjelderSøker,
                                              barnasFødselsdatoer,
                                              målform)
            }har oppholdstillatelse fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi${
                duOgEllerBarnaFødtFormulering(gjelderSøker,
                                              barnasFødselsdatoer,
                                              målform)
            }har opphaldsløyve frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

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

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du har oppholdsrett fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du har opphaldsrett frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_OMSORG_FOR_BARN("Adopsjon, surrogati: Omsorgen for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har omsorgen for barn født ${barnasFødselsdatoer.tilBrevTekst()} fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi du har omsorga for barn fødd ${barnasFødselsdatoer.tilBrevTekst()} frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_BOR_HOS_SØKER("Barn har flyttet til søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} bor hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} bur hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at barn født ${barnasFødselsdatoer.tilBrevTekst()} bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at barn fødd ${barnasFødselsdatoer.tilBrevTekst()} bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_NYFØDT_BARN("Nyfødt barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

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

    INNVILGET_MEDLEM_I_FOLKETRYGDEN("Medlem i Folketrygden") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du er medlem i Norsk Folketrygd fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi du er medlem i Norsk Folketrygd frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} bor sammen med deg."
            Målform.NN -> "Du får barnetrygd fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} bur saman med deg."
        }
    },
    REDUKSJON_BOSATT_I_RIKTET("Barn har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
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
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke bor hos deg $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje bur hos deg $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_BARN_DØD(tittel = "Barn død", erTilgjengeligFrontend = true) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} døde i $månedOgÅrBegrunnelsenGjelderFor. "
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} døydde i $månedOgÅrBegrunnelsenGjelderFor. "
                }
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN("Søker har ikke lenger fast omsorg for barn: Beredskapshjem, fosterhjem, institusjon, vurdering fast bosted mellom foreldrene") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
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
    REDUKSJON_MANGLENDE_OPPLYSNINGER(tittel = "Ikke mottatt dokumentasjon", erTilgjengeligFrontend = true) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
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
    REDUKSJON_UNDER_18_ÅR("Barn har fylt 18 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} fylte 18 år."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fylte 18 år. "
                }
    },
    REDUKSJON_UNDER_6_ÅR("Barn har fylt 6 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(10)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} fyller 6 år."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fyller 6 år."
                }
    },
    REDUKSJON_DELT_BOSTED_ENIGHET("Enighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
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
    REDUKSJON_ENDRET_MOTTAKER("Foreldrene bor sammen, endret mottaker", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
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
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = ""
    },
    INNVILGET_SATSENDRING("Satsendring") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE

        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(10)
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
                    Målform.NN -> "Barnetrygd for barn født ${barnasFødselsdatoer.tilBrevTekst()} fordi ${
                        duOgEllerBarnetBarnaFormulering(gjelderSøker, barnasFødselsdatoer)
                                .trim()
                    } ikkje er busett i Noreg${fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)}."
                }
    },
    AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER("Tredjelandsborger uten lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
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
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi vi har kommet fram til at du ikke har oppholdsrett som EØS-borger${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd fordi vi har kome fram til at du ikkje har opphaldsrett som EØS-borgar${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER("Skjønnsmessig vurdering opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi vi har kommet fram til at du ikke har oppholdsrett i Norge${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd fordi vi har komme fram til at du ikkje har opphaldsrett i Noreg${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_MEDLEM_I_FOLKETRYGDEN("Unntatt medlemskap i Folketrygden") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi du ikke er medlem av folketrygden${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                    Målform.NN -> "Barnetrygd fordi du ikkje er medlem av folketrygda${
                        fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor, målform)
                    }."
                }
    },
    AVSLAG_FORELDRENE_BOR_SAMMEN("Foreldrene bor sammen", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(12)
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
    AVSLAG_OPPLYSNINGSPLIKT("Ikke mottatt opplysninger") { // TODO : Høre med Meng

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
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
    AVSLAG_UREGISTRERT_BARN("Barn uten fødselsnummer", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygd fordi du har søkt for barn som ikke er registrert i folkeregisteret."
                    Målform.NN -> "Barnetrygd fordi du har søkt for barn som ikkje er registrert i folkeregisteret."
                }
    },
    AVSLAG_FRITEKST("Fritekst", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf()
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = ""
    },
    OPPHØR_BARN_FLYTTET_FRA_SØKER("Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født ${barnasFødselsdatoer.tilBrevTekst()} har flyttet fra deg i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd ${barnasFødselsdatoer.tilBrevTekst()} har flytta frå deg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_SØKER_FLYTTET_FRA_BARN("Søker har flyttet fra barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du i $månedOgÅrBegrunnelsenGjelderFor flyttet fra barn født ${barnasFødselsdatoer.tilBrevTekst()}."
                    Målform.NN -> "Du i $månedOgÅrBegrunnelsenGjelderFor flytta frå barn fødd ${barnasFødselsdatoer.tilBrevTekst()}."
                }
    },
    OPPHØR_BARN_UTVANDRET("Barn har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født ${barnasFødselsdatoer.tilBrevTekst()} har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd ${barnasFødselsdatoer.tilBrevTekst()} har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_SØKER_UTVANDRET("Søker har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Du har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_BARN_DØD(tittel = "Barn død", erTilgjengeligFrontend = true) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "${barnasFødselsdatoer.barnetBarnaDineDittFormulering().storForbokstav()} som er født ${barnasFødselsdatoer.tilBrevTekst()} døde. Barnetrygden opphører fra måneden etter at ${barnasFødselsdatoer.barnetBarnaFormulering()} døde."
                    Målform.NN -> "${barnasFødselsdatoer.barnetBarnaDineDittFormulering().storForbokstav()} som er fødd ${barnasFødselsdatoer.tilBrevTekst()} døydde. Barnetrygda opphøyrer frå månaden etter at ${barnasFødselsdatoer.barnetBarnaFormulering()} døydde."
                }
    },
    OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG("Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
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
    OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE("Barn har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født ${barnasFødselsdatoer.tilBrevTekst()} ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd ${barnasFødselsdatoer.tilBrevTekst()} ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE("Søker har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Du ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_IKKE_MOTTATT_OPPLYSNINGER(tittel = "Ikke mottatt opplysninger", erTilgjengeligFrontend = true) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
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
    OPPHØR_UNDER_18_ÅR("Barn har fylt 18 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden opphør fordi barn født ${barnasFødselsdatoer.tilBrevTekst()} fylte 18 år."
                    Målform.NN -> "Barnetrygda er opphørt fordi barn fødd ${barnasFødselsdatoer.tilBrevTekst()} fylte 18 år. "
                }
    },
    OPPHØR_FRITEKST("Fritekst", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf()
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: List<LocalDate>,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = ""
    },
    FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET("Søker og barn oppholder seg i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
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
    FORTSATT_INNVILGET_FRITEKST("Fortsatt innvilget fritekst", erTilgjengeligFrontend = false) {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf()
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

    companion object {

        fun VedtakBegrunnelseSpesifikasjon.finnVilkårFor(): Vilkår? {
            val vilkårForBegrunnelse =
                    VedtakBegrunnelseUtils.vilkårMedVedtakBegrunnelser.filter { it.value.contains(this) }.map { it.key }
            return if (vilkårForBegrunnelse.size > 1) error("Begrunnelser kan kun være tilknyttet et vilkår, men begrunnelse ${this.name} er knyttet til flere: $vilkårForBegrunnelse")
            else vilkårForBegrunnelse.singleOrNull()
        }

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

        fun fraOgTilFormulering(månedOgÅrBegrunnelsenGjelderFor: String, målform: Målform) =
                when (målform) {
                    Målform.NB -> if (månedOgÅrBegrunnelsenGjelderFor.isNotBlank()) " fra $månedOgÅrBegrunnelsenGjelderFor" else ""
                    Målform.NN -> if (månedOgÅrBegrunnelsenGjelderFor.isNotBlank()) " frå $månedOgÅrBegrunnelsenGjelderFor" else ""
                }
    }
}

val hjemlerTilhørendeFritekst = setOf(2, 4, 11)

enum class VedtakBegrunnelseType {
    INNVILGELSE,
    REDUKSJON,
    AVSLAG,
    OPPHØR,
    FORTSATT_INNVILGET
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
    else -> periode.fom.forrigeMåned().tilMånedÅr()
}