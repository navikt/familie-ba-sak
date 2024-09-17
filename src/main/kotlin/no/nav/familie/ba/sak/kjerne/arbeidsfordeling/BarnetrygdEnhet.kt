package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

enum class BarnetrygdEnhet(
    val enhetsnummer: String,
) {
    VIKAFOSSEN("2103"),
    DRAMMEN("4806"),
    VADSØ("4820"),
    OSLO("4833"),
    STORD("4842"),
    STEINKJER("4817"),
    BERGEN("4812"),
    MIDLERTIDIG_ENHET("4863"),
    ;

    companion object {
        const val VIKAFOSSEN_ENHET_2103_NAVN = "NAV Vikafossen"
        private val GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER: List<BarnetrygdEnhet> = listOf(VIKAFOSSEN, DRAMMEN, VADSØ, OSLO, STORD, STEINKJER, BERGEN)

        fun erGyldigBehandlendeBarnetrygdEnhet(enhetsnummer: String): Boolean = GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER.any { it.enhetsnummer == enhetsnummer }
    }
}
