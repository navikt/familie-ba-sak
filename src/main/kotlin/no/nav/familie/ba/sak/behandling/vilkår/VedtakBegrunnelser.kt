package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.tilMånedÅr
import java.util.*

interface IVedtakBegrunnelse {

    val vedtakBegrunnelseType: VedtakBegrunnelseType
    fun hentHjemler(): SortedSet<Int>
    fun hentBeskrivelse(
            gjelderSøker: Boolean = false,
            barnasFødselsdatoer: String = "",
            månedOgÅrBegrunnelsenGjelderFor: String = "",
            målform: Målform
    ): String
}

enum class VedtakBegrunnelser(val tittel: String) : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET("Norsk, nordisk, tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}er bosatt i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn fødd $barnasFødselsdatoer " else ""}er busett i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}har oppholdstillatelse fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn fødd $barnasFødselsdatoer " else ""}har opphaldsløyve frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
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
                barnasFødselsdatoer: String,
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
                barnasFødselsdatoer: String,
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
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har omsorgen for barn født $barnasFødselsdatoer fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi du har omsorga for barn fødd $barnasFødselsdatoer frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_BOR_HOS_SØKER("Barn har flyttet til søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi barn født $barnasFødselsdatoer bor hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi barn fødd $barnasFødselsdatoer bur hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at barn født $barnasFødselsdatoer bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at barn fødd $barnasFødselsdatoer bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
        }
    },
    INNVILGET_NYFØDT_BARN("Nyfødt barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
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
                barnasFødselsdatoer: String,
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
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi barn født $barnasFødselsdatoer bor sammen med deg."
            Målform.NN -> "Du får barnetrygd fordi barn fødd $barnasFødselsdatoer bur saman med deg."
        }
    },
    REDUKSJON_BOSATT_I_RIKTET("Barn har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN("Barn har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_FLYTTET_FORELDER("Søker har flyttet fra barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi du i $månedOgÅrBegrunnelsenGjelderFor flyttet fra barn født $barnasFødselsdatoer."
                    Målform.NN -> "Barnetrygda er redusert fordi du i $månedOgÅrBegrunnelsenGjelderFor flyttå frå barn fødd $barnasFødselsdatoer."
                }
    },
    REDUKSJON_FLYTTET_BARN("Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer har flyttet fra deg $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer har flytta frå deg $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_BARN_DØD("Barn død") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String, // TODO: [BARNS DØDSDATO]
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer døde i $månedOgÅrBegrunnelsenGjelderFor. "
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer døydde i $månedOgÅrBegrunnelsenGjelderFor. "
                }
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN("Søker har ikke lenger fast omsorg for barn: Beredskapshjem, fosterhjem, institusjon, vurdering fast bosted mellom foreldrene") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi vi har kommet fram til at barn født $barnasFødselsdatoer ikke lenger bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi vi har kome fram til at barn fødd $barnasFødselsdatoer ikkje lenger bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_MANGLENDE_OPPLYSNINGER("Ikke mottatt dokumentasjon") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi du ikke har sendt oss de opplysningene vi ba om for barn født $barnasFødselsdatoer."
                    Målform.NN -> "Barnetrygda er redusert fordi du ikkje har sendt oss dei opplysningane vi ba om for barn fødd  $barnasFødselsdatoer."
                }
    },
    REDUKSJON_UNDER_18_ÅR("Barn har fylt 18 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 10)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer fylte 18 år."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer fylte 18 år. "
                }
    },
    REDUKSJON_UNDER_6_ÅR("Barn har fylt 6 år") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 10)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer fyller 6 år."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer fyller 6 år."
                }
    },
    REDUKSJON_DELT_BOSTED_ENIGHET("Enighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi avtalen om delt bosted for barn født $barnasFødselsdatoer er opphørt fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barnetrygda er redusert fordi avtalen om delt bustad for barn fødd $barnasFødselsdatoer er opphøyrt frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    REDUKSJON_DELT_BOSTED_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du og den andre forelderen er uenige om avtalen om delt bosted. Vi har kommet fram til at avtalen om delt bosted for barn født $barnasFødselsdatoer ikke lenger praktiseres fra $månedOgÅrBegrunnelsenGjelderFor." +
                                  "\nVed uenighet mellom foreldrene om avtalen om delt bosted, kan barnetrygden opphøres fra måneden etter at vi fikk søknad om full barnetrygd."
                    Målform.NN -> "Du og den andre forelderen er usamde om avtalen om delt bustad. Vi har kome fram til at avtalen om delt bustad for barn fødd $barnasFødselsdatoer ikkje lenger blir praktisert frå $månedOgÅrBegrunnelsenGjelderFor." +
                                  "\nNår de er usamde om avtalen om delt bustad, kan vi opphøyre barnetrygda til deg frå og med månaden etter at vi fekk søknad om full barnetrygd. "
                }
    },
    INNVILGET_SATSENDRING("Satsendring") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE

        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(10)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden endres fordi det har vært en satsendring."
                    Målform.NN -> "Barnetrygda er endra fordi det har vore ei satsendring."
                }
    },
    OPPHØR_BARN_FLYTTET_FRA_SØKER("Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer har flyttet fra deg i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer har flytta frå deg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_SØKER_FLYTTET_FRA_BARN("Søker har flyttet fra barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du i $månedOgÅrBegrunnelsenGjelderFor flyttet fra barn født $barnasFødselsdatoer."
                    Målform.NN -> "Du i $månedOgÅrBegrunnelsenGjelderFor flytta frå barn fødd $barnasFødselsdatoer."
                }
    },
    OPPHØR_BARN_UTVANDRET("Barn har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_SØKER_UTVANDRET("Søker har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du har flyttet fra Norge i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Du har flytta frå Noreg i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_BARN_DØD("Barn død") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String, // TODO: [BARNS DØDSDATO]
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer døde i $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer døydde i $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG("Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Vi har kommet fram til at barn født $barnasFødselsdatoer ikke lenger bor fast hos deg fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Vi har kome fram til at barn fødd $barnasFødselsdatoer ikkje lenger bur fast hos deg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE("Barn har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE("Søker har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du ikke lenger har oppholdstillatelse i Norge fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Du ikkje lenger har opphaldsløyve i Noreg frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_IKKE_MOTTATT_OPPLYSNINGER("Ikke mottatt opplysninger") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
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
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Avtalen om delt bosted for barn født $barnasFødselsdatoer  er opphørt fra $månedOgÅrBegrunnelsenGjelderFor."
                    Målform.NN -> "Avtalen om delt bustad for barn fødd $barnasFødselsdatoer  er opphøyrt frå $månedOgÅrBegrunnelsenGjelderFor."
                }
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                månedOgÅrBegrunnelsenGjelderFor: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du og den andre forelderen er uenige om avtalen om delt bosted. Vi har kommet fram til at avtale om delt bosted for barn født $barnasFødselsdatoer ikke lenger praktiseres fra $månedOgÅrBegrunnelsenGjelderFor.\r" +
                                  "Ved uenighet mellom foreldrene om avtalen om delt bosted, kan barnetrygden opphøres fra måneden etter at vi fikk søknad om full barnetrygd."
                    Målform.NN -> "Du og den andre forelderen er usamde om avtalen om delt bustad. Vi har kome fram til at avtalen om delt bustad for barn fødd $barnasFødselsdatoer ikkje lenger blir praktisert frå $månedOgÅrBegrunnelsenGjelderFor.\r" +
                                  "Når de er usamde om avtalen om delt bustad, kan vi opphøyre barnetrygda til deg frå og med månaden etter at vi fekk søknad om full barnetrygd."
                }
    };

    companion object {

        fun VedtakBegrunnelser.finnVilkårFor(): Vilkår? = VedtakBegrunnelseUtils.vilkårBegrunnelser
                .filter { it.value.contains(this) }
                .map { it.key }
                .singleOrNull()
    }
}

enum class VedtakBegrunnelseType {
    INNVILGELSE,
    REDUKSJON,
    OPPHØR
}

fun VedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(periode: Periode) = when (this) {
    VedtakBegrunnelseType.REDUKSJON -> periode.fom.minusMonths(1).tilMånedÅr()
    VedtakBegrunnelseType.OPPHØR -> periode.tom.tilMånedÅr()
    else -> periode.fom.minusMonths(1).tilMånedÅr()
}