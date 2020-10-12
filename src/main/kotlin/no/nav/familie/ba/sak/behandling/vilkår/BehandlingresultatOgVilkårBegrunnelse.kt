package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform

interface IVedtakBegrunnelse {

    fun hentHjemler(): Set<Int>

    fun hentBeskrivelse(gjelderSøker: Boolean = false,
                        barnasFødselsdatoer: String = "",
                        vilkårsdato: String,
                        målform: Målform): String
}

enum class BehandlingresultatOgVilkårBegrunnelse(val tittel: String) : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET("Norsk, nordisk, tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override fun hentHjemler(): Set<Int> = setOf(2, 4, 11)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}er bosatt i Norge fra $vilkårsdato."
            Målform.NN -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn fødd $barnasFødselsdatoer " else ""}er busett i Noreg frå $vilkårsdato."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override fun hentHjemler(): Set<Int> = setOf(2, 4, 11)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}har oppholdstillatelse fra $vilkårsdato."
            Målform.NN -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn fødd $barnasFødselsdatoer " else ""}har opphaldsløyve frå $vilkårsdato."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override fun hentHjemler(): Set<Int> = setOf(2, 4, 11)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har oppholdsrett som EØS-borger fra $vilkårsdato."
            Målform.NN -> "Du får barnetrygd fordi du har opphaldsrett som EØS-borgar frå $vilkårsdato."
        }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING("EØS-borger: Skjønnsmessig vurdering av oppholdsrett.") {

        override fun hentHjemler(): Set<Int> = setOf(2, 4, 11)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du har oppholdsrett som EØS-borger fra $vilkårsdato."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at du har opphaldsrett som EØS-borgar frå $vilkårsdato."
        }
    },
    INNVILGET_OMSORG_FOR_BARN("Adopsjon, surrogati: Omsorgen for barn") {

        override fun hentHjemler(): Set<Int> = setOf(2, 4, 11)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi du har omsorgen for barn født $barnasFødselsdatoer fra $vilkårsdato."
            Målform.NN -> "Du får barnetrygd fordi du har omsorga for barn fødd $barnasFødselsdatoer frå $vilkårsdato."
        }
    },
    INNVILGET_BOR_HOS_SØKER("Barn har flyttet til søker") {

        override fun hentHjemler(): Set<Int> = setOf(2, 4, 11)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi barn født $barnasFødselsdatoer bor hos deg fra $vilkårsdato."
            Målform.NN -> "Du får barnetrygd fordi barn fødd $barnasFødselsdatoer bur hos deg frå $vilkårsdato."
        }
    },
    INNVILGET_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override fun hentHjemler(): Set<Int> = setOf(2, 4, 11)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at barn født $barnasFødselsdatoer bor fast hos deg fra $vilkårsdato."
            Målform.NN -> "Du får barnetrygd fordi vi har kome fram til at barn fødd $barnasFødselsdatoer bur fast hos deg frå $vilkårsdato."
        }
    },
    SATSENDRING("Satsendring") {

        override fun hentHjemler(): Set<Int> = setOf(10)

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String = when (målform) {
            Målform.NB -> "Barnetrygden er endret fordi det har vært en satsendring."
            Målform.NN -> "Barnetrygda er endra fordi det har vore ei satsendring."
        }
    },
}