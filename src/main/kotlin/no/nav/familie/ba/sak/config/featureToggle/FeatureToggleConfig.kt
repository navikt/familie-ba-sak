package no.nav.familie.ba.sak.config

class FeatureToggleConfig {
    companion object {

        const val BRUK_ANDELER_FOR_IVERKSETTELSE_SJEKK = "familie-ba-sak.bruk-andeler-for-iverksettelse-sjekk"
        const val BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER = "familie-ba-sak.frikoble-andeler-og-endringer"
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val SKATTEETATEN_API_EKTE_DATA = "familie-ba-sak.skatteetaten-api-ekte-data-i-respons"
        const val IKKE_STOPP_MIGRERINGSBEHANDLING = "familie-ba-sak.ikke.stopp.migeringsbehandling"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"
        const val ENDRINGER_I_VALIDERING_FOR_MIGRERINGSBEHANDLING =
            "familie-ba-sak.endringer.validering.migeringsbehandling"
        const val NY_MÅTE_Å_BEREGNE_BEHANDLINGSRESULTAT = "familie-ba-sak.behandling.behandlingsresultat"

        const val MIGRERING_MED_FEILUTBETALING_UNDER_BELØPSGRENSE =
            "familie-ba-sak.migrering-med-feilutbetaling-under-belopsgrense"
        const val KAN_GENERERE_UTBETALINGSOPPDRAG_NY = "familie-ba-sak.generer.utbetalingsoppdrag.ny"

        const val TREKK_I_LØPENDE_UTBETALING = "familie-ba-sak.trekk-i-loepende-utbetaling"
        const val EØS_INFORMASJON_OM_ÅRLIG_KONTROLL = "familie-ba-sak.eos-informasjon-om-aarlig-kontroll"

        const val ER_MANUEL_POSTERING_TOGGLE_PÅ = "familie-ba-sak.manuell-postering"

        // unleash toggles for satsendring, kan slettes etter at satsendring er skrudd på for alle satstyper
        const val SATSENDRING_ENABLET: String = "familie-ba-sak.satsendring-enablet"
        const val SATSENDRING_OPPRETT_TASKER = "familie-ba-sak.satsendring-opprett-satsendring-task"
        const val SATSENDRING_SJEKK_UTBETALING = "familie-ba-sak.satsendring-sjekk-utbetaling"
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
