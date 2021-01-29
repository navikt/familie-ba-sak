package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import java.util.*

interface IVedtakBegrunnelse {

    val vedtakBegrunnelseType: VedtakBegrunnelseType
    fun hentHjemler(): SortedSet<Int>
    fun hentBeskrivelse(
            gjelderSøker: Boolean = false,
            barnasFødselsdatoer: String = "",
            vilkårMånedÅr: String = "",
            målform: Målform
    ): String
}

enum class VedtakBegrunnelse(val tittel: String) : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET("Norsk, nordisk, tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11, 2)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}er bosatt i Norge fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn fødd $barnasFødselsdatoer " else ""}er busett i Noreg frå $vilkårMånedÅr."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}har oppholdstillatelse fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn fødd $barnasFødselsdatoer " else ""}har opphaldsløyve frå $vilkårMånedÅr."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har oppholdsrett som EØS-borger fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi du har opphaldsrett som EØS-borgar frå $vilkårMånedÅr."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING("EØS-borger: Skjønnsmessig vurdering av oppholdsrett.") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du har oppholdsrett som EØS-borger fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du har opphaldsrett som EØS-borgar frå $vilkårMånedÅr."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER("Skjønnsmessig vurdering tålt opphold tredjelandsborger") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du har oppholdsrett fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du har opphaldsrett frå $vilkårMånedÅr."
        }
    },
    INNVILGET_OMSORG_FOR_BARN("Adopsjon, surrogati: Omsorgen for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har omsorgen for barn født $barnasFødselsdatoer fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi du har omsorga for barn fødd $barnasFødselsdatoer frå $vilkårMånedÅr."
        }
    },
    INNVILGET_BOR_HOS_SØKER("Barn har flyttet til søker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi barn født $barnasFødselsdatoer bor hos deg fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi barn fødd $barnasFødselsdatoer bur hos deg frå $vilkårMånedÅr."
        }
    },
    INNVILGET_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at barn født $barnasFødselsdatoer bor fast hos deg fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at barn fødd $barnasFødselsdatoer bur fast hos deg frå $vilkårMånedÅr."
        }
    },
    INNVILGET_NYFØDT_BARN("Nyfødt barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
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
                vilkårMånedÅr: String,
                målform: Målform
        ): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du er medlem i Norsk Folketrygd fra $vilkårMånedÅr."
            Målform.NN -> "Du får barnetrygd fordi du er medlem i Norsk Folketrygd frå $vilkårMånedÅr."
        }
    },
    INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER("Foreldrene bor sammen, endret mottaker") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)

        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
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
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer har flyttet fra Norge i $vilkårMånedÅr."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer har flytta frå Noreg i $vilkårMånedÅr."
                }
    },
    REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN("Barn har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer ikke lenger har oppholdstillatelse i Norge fra $vilkårMånedÅr."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer ikkje lenger har opphaldsløyve i Noreg frå $vilkårMånedÅr."
                }
    },
    REDUKSJON_FLYTTET_FORELDER("Søker har flyttet fra barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi du i $vilkårMånedÅr flyttet fra barn født $barnasFødselsdatoer."
                    Målform.NN -> "Barnetrygda er redusert fordi du i $vilkårMånedÅr flyttå frå barn fødd $barnasFødselsdatoer."
                }
    },
    REDUKSJON_FLYTTET_BARN("Barn har flyttet fra søker (flytting mellom foreldre, andre omsorgspersoner)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer har flyttet fra deg $vilkårMånedÅr."
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer har flytta frå deg $vilkårMånedÅr."
                }
    },
    REDUKSJON_BARN_DØD("Barn død") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String, // TODO: [BARNS DØDSDATO]
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer døde i $vilkårMånedÅr. "
                    Målform.NN -> "Barnetrygda er redusert fordi barn fødd $barnasFødselsdatoer døydde i $vilkårMånedÅr. "
                }
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN("Søker har ikke lenger fast omsorg for barn: Beredskapshjem, fosterhjem, institusjon, vurdering fast bosted mellom foreldrene") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi vi har kommet fram til at barn født $barnasFødselsdatoer ikke lenger bor fast hos deg fra $vilkårMånedÅr."
                    Målform.NN -> "Barnetrygda er redusert fordi vi har kome fram til at barn fødd $barnasFødselsdatoer ikkje lenger bur fast hos deg frå $vilkårMånedÅr."
                }
    },
    REDUKSJON_MANGLENDE_OPPLYSNINGER("Ikke mottatt dokumentasjon") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
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
                vilkårMånedÅr: String,
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
                vilkårMånedÅr: String,
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
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi avtalen om delt bosted for barn født $barnasFødselsdatoer er opphørt fra $vilkårMånedÅr."
                    Målform.NN -> "Barnetrygda er redusert fordi avtalen om delt bustad for barn fødd $barnasFødselsdatoer er opphøyrt frå $vilkårMånedÅr."
                }
    },
    REDUKSJON_DELT_BOSTED_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du og den andre forelderen er uenige om avtalen om delt bosted. Vi har kommet fram til at avtalen om delt bosted for barn født $barnasFødselsdatoer ikke lenger praktiseres fra $vilkårMånedÅr." +
                                  "\nVed uenighet mellom foreldrene om avtalen om delt bosted, kan barnetrygden opphøres fra måneden etter at vi fikk søknad om full barnetrygd."
                    Målform.NN -> "Du og den andre forelderen er usamde om avtalen om delt bustad. Vi har kome fram til at avtalen om delt bustad for barn fødd $barnasFødselsdatoer ikkje lenger blir praktisert frå $vilkårMånedÅr." +
                                  "\nNår de er usamde om avtalen om delt bustad, kan vi opphøyre barnetrygda til deg frå og med månaden etter at vi fekk søknad om full barnetrygd. "
                }
    },
    INNVILGET_SATSENDRING("Satsendring") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE

        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(10)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
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
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer har flyttet fra deg i $vilkårMånedÅr."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer har flytta frå deg i $vilkårMånedÅr."
                }
    },
    OPPHØR_SØKER_FLYTTET_FRA_BARN("Søker har flyttet fra barn") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du i $vilkårMånedÅr flyttet fra barn født $barnasFødselsdatoer."
                    Målform.NN -> "Du i $vilkårMånedÅr flytta frå barn fødd $barnasFødselsdatoer."
                }
    },
    OPPHØR_BARN_UTVANDRET("Barn har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer har flyttet fra Norge i $vilkårMånedÅr."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer har flytta frå Noreg i $vilkårMånedÅr."
                }
    },
    OPPHØR_SØKER_UTVANDRET("Søker har flyttet fra Norge") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du har flyttet fra Norge i $vilkårMånedÅr."
                    Målform.NN -> "Du har flytta frå Noreg i $vilkårMånedÅr."
                }
    },
    OPPHØR_BARN_DØD("Barn død") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String, // TODO: [BARNS DØDSDATO]
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer døde i $vilkårMånedÅr."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer døydde i $vilkårMånedÅr."
                }
    },
    OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG("Søker har ikke lenger fast omsorg for barn (beredskapshjem, vurdering av fast bosted)") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Vi har kommet fram til at barn født $barnasFødselsdatoer ikke lenger bor fast hos deg fra $vilkårMånedÅr."
                    Målform.NN -> "Vi har kome fram til at barn fødd $barnasFødselsdatoer ikkje lenger bur fast hos deg frå $vilkårMånedÅr."
                }
    },
    OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE("Barn har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Barn født $barnasFødselsdatoer ikke lenger har oppholdstillatelse i Norge fra $vilkårMånedÅr."
                    Målform.NN -> "Barn fødd $barnasFødselsdatoer ikkje lenger har opphaldsløyve i Noreg frå $vilkårMånedÅr."
                }
    },
    OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE("Søker har ikke oppholdstillatelse") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(4, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du ikke lenger har oppholdstillatelse i Norge fra $vilkårMånedÅr."
                    Målform.NN -> "Du ikkje lenger har opphaldsløyve i Noreg frå $vilkårMånedÅr."
                }
    },
    OPPHØR_IKKE_MOTTATT_OPPLYSNINGER("Ikke mottatt opplysninger") { // TODO: Ikke støttet enda

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(17, 18)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
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
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Avtalen om delt bosted for barn født $barnasFødselsdatoer  er opphørt fra $vilkårMånedÅr."
                    Målform.NN -> "Avtalen om delt bustad for barn fødd $barnasFødselsdatoer  er opphøyrt frå $vilkårMånedÅr."
                }
    },
    OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET("Uenighet om opphør av avtale om delt bosted") {

        override val vedtakBegrunnelseType = VedtakBegrunnelseType.OPPHØR
        override fun hentHjemler(): SortedSet<Int> = sortedSetOf(2, 11)
        override fun hentBeskrivelse(
                gjelderSøker: Boolean,
                barnasFødselsdatoer: String,
                vilkårMånedÅr: String,
                målform: Målform
        ): String =
                when (målform) {
                    Målform.NB -> "Du og den andre forelderen er uenige om avtalen om delt bosted. Vi har kommet fram til at avtale om delt bosted for barn født $barnasFødselsdatoer ikke lenger praktiseres fra $vilkårMånedÅr.\r" +
                                  "Ved uenighet mellom foreldrene om avtalen om delt bosted, kan barnetrygden opphøres fra måneden etter at vi fikk søknad om full barnetrygd."
                    Målform.NN -> "Du og den andre forelderen er usamde om avtalen om delt bustad. Vi har kome fram til at avtalen om delt bustad for barn fødd $barnasFødselsdatoer ikkje lenger blir praktisert frå $vilkårMånedÅr.\r" +
                                  "Når de er usamde om avtalen om delt bustad, kan vi opphøyre barnetrygda til deg frå og med månaden etter at vi fekk søknad om full barnetrygd."
                }
    };

    companion object {

        fun VedtakBegrunnelse.finnVilkårFor(): Vilkår? = VedtakBegrunnelseSerivce.vilkårBegrunnelser
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