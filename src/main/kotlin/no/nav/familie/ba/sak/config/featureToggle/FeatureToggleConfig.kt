package no.nav.familie.ba.sak.config

class FeatureToggleConfig {
    companion object {
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val SKATTEETATEN_API_EKTE_DATA = "familie-ba-sak.skatteetaten-api-ekte-data-i-respons"
        const val IKKE_STOPP_MIGRERINGSBEHANDLING = "familie-ba-sak.ikke.stopp.migeringsbehandling"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"
        const val ENDRINGER_I_VALIDERING_FOR_MIGRERINGSBEHANDLING =
            "familie-ba-sak.endringer.validering.migeringsbehandling"

        const val KAN_GENERERE_UTBETALINGSOPPDRAG_NY = "familie-ba-sak.generer.utbetalingsoppdrag.ny"

        const val TREKK_I_LØPENDE_UTBETALING = "familie-ba-sak.trekk-i-loepende-utbetaling"
        const val EØS_INFORMASJON_OM_ÅRLIG_KONTROLL = "familie-ba-sak.eos-informasjon-om-aarlig-kontroll"
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
