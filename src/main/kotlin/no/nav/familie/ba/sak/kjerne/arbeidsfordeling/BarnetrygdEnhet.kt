package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.common.Feil

enum class BarnetrygdEnhet(
    val enhetsnummer: String,
    val enhetsnavn: String,
    val gruppenavn: String,
) {
    VIKAFOSSEN("2103", "Nav Vikafossen", "0000-GA-ENHET_2103"),
    DRAMMEN("4806", "Nav familie- og pensjonsytelser Drammen", "0000-GA-ENHET_4806"),
    VADSØ("4820", "Nav familie- og pensjonsytelser Vadsø", "0000-GA-ENHET_4820"),
    OSLO("4833", "Nav familie- og pensjonsytelser Oslo 1", "0000-GA-ENHET_4833"),
    STORD("4842", "Nav familie- og pensjonsytelser Stord", "0000-GA-ENHET_4842"),
    STEINKJER("4817", "Nav familie- og pensjonsytelser Steinkjer", "0000-GA-ENHET_4817"),
    MIDLERTIDIG_ENHET("4863", "Midlertidig enhet", "0000-GA-ENHET_4863"),
    ;

    override fun toString(): String = "$enhetsnavn ($enhetsnummer)"

    companion object {
        const val AUTOMATISK_BEHANDLING_BREVSIGNATUR = "Nav familie- og pensjonsytelser"
        private val GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER: List<BarnetrygdEnhet> =
            listOf(
                VIKAFOSSEN,
                DRAMMEN,
                VADSØ,
                OSLO,
                STORD,
                STEINKJER,
            )

        fun erGyldigBehandlendeBarnetrygdEnhet(enhetsnummer: String): Boolean = GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER.any { it.enhetsnummer == enhetsnummer }

        fun fraEnhetsnummer(enhetsnummer: String): BarnetrygdEnhet = entries.firstOrNull { it.enhetsnummer == enhetsnummer } ?: throw Feil("Finner ikke enhet med enhetsnummer $enhetsnummer")
    }
}
