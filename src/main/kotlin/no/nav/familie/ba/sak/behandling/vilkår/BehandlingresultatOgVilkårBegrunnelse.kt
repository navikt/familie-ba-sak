package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform

interface IVedtakBegrunnelse {

    fun hentBeskrivelse(gjelderSøker: Boolean = false,
                        barnasFødselsdatoer: String = "",
                        vilkårsdato: String,
                        målform: Målform): String
}

enum class BehandlingresultatOgVilkårBegrunnelse(val tittel: String) : IVedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET("Norsk, nordisk, tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}er bosatt i Norge fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}har oppholdstillatelse fra $vilkårsdato."
                    Målform.NN -> ""
                    Målform.NN -> ""
                }
    },
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du har oppholdsrett som EØS-borger fra $vilkårsdato."
                    Målform.NN -> ""
                }

    },
    INNVILGET_LOVLIG_OPPHOLD_AAREG("EØS-borger: Søker arbeider eller har ytelser fra NAV") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du arbeider eller får utbetalinger fra NAV som er det samme som arbeidsinntekt."
                    Målform.NN -> ""
                }
    },
    INNVILGET_OMSORG_FOR_BARN("Adopsjon, surrogati: Omsorgen for barn") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du har omsorgen for barn født $barnasFødselsdatoer fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    INNVILGET_BOR_HOS_SØKER("Barn har flyttet til søker") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi barn født $barnasFødselsdatoer bor hos deg fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    INNVILGET_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du har fått fast omsorg for barn født $barnasFødselsdatoer fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    REDUKSJON_BOSATT_I_RIKTET("Norsk, nordisk, tredjelandsborger med lovlig opphold samtidig som bosatt i Norge") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}er bosatt i Norge fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE("Tredjelandsborger bosatt før lovlig opphold i Norge") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}har oppholdstillatelse fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    REDUKSJON_LOVLIG_OPPHOLD_EØS_BORGER("EØS-borger: Søker har oppholdsrett") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du har oppholdsrett som EØS-borger fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    REDUKSJON_LOVLIG_OPPHOLD_AAREG("EØS-borger: Søker arbeider eller har ytelser fra NAV") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du arbeider eller får utbetalinger fra NAV som er det samme som arbeidsinntekt."
                    Målform.NN -> ""
                }
    },
    REDUKSJON_OMSORG_FOR_BARN("Adopsjon, surrogati: Omsorgen for barn") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du har omsorgen for barn født $barnasFødselsdatoer fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    REDUKSJON_BOR_HOS_SØKER("Barn har flyttet til søker") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi barn født $barnasFødselsdatoer bor hos deg fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    REDUKSJON_FAST_OMSORG_FOR_BARN("Søker har fast omsorg for barn") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi vi har kommet fram til at du har fått fast omsorg for barn født $barnasFødselsdatoer fra $vilkårsdato."
                    Målform.NN -> ""
                }
    },
    SATSENDRING("Satsendring") {

        override fun hentBeskrivelse(gjelderSøker: Boolean,
                                     barnasFødselsdatoer: String,
                                     vilkårsdato: String,
                                     målform: Målform): String =
                when (målform) {
                    Målform.NB -> "Barnetrygden endres fordi det har vært en satsendring."
                    Målform.NN -> ""
                }
    },
}