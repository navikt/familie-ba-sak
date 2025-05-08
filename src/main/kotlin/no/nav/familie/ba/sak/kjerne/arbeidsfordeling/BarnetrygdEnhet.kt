package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties
class EnhetConfig {
    var enheter: Map<String, String> = emptyMap()

    fun hentAlleEnheterBrukerHarTilgangTil() =
        enheter
            .filter { SikkerhetContext.hentGrupper().contains(it.value) }
            .mapNotNull { (key, _) -> runCatching { BarnetrygdEnhet.valueOf(key) }.getOrNull() }
}

enum class BarnetrygdEnhet(
    val enhetsnummer: String,
    val enhetsnavn: String,
) {
    VIKAFOSSEN("2103", "NAV Vikafossen"),
    DRAMMEN("4806", "NAV Familie- og pensjonsytelser Drammen"),
    VADSO("4820", "NAV Familie- og pensjonsytelser Vadsø"),
    OSLO("4833", "NAV Familie- og pensjonsytelser Oslo 1"),
    STORD("4842", "NAV Familie- og pensjonsytelser Stord"),
    STEINKJER("4817", "NAV Familie- og pensjonsytelser Steinkjer"),
    MIDLERTIDIG_ENHET("4863", "Midlertidig enhet"),
    ;

    override fun toString(): String = "$enhetsnavn ($enhetsnummer)"

    companion object {
        private val GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER: List<BarnetrygdEnhet> =
            listOf(
                VIKAFOSSEN,
                DRAMMEN,
                VADSO,
                OSLO,
                STORD,
                STEINKJER,
            )

        fun erGyldigBehandlendeBarnetrygdEnhet(enhetsnummer: String): Boolean = GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER.any { it.enhetsnummer == enhetsnummer }
    }
}
