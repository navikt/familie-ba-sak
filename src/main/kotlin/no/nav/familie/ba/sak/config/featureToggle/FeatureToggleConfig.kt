package no.nav.familie.ba.sak.config

class FeatureToggleConfig {
    companion object {
        // Operasjonelle
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val SKATTEETATEN_API_EKTE_DATA = "familie-ba-sak.skatteetaten-api-ekte-data-i-respons"
        const val IKKE_STOPP_MIGRERINGSBEHANDLING = "familie-ba-sak.ikke.stopp.migeringsbehandling"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"
        const val BRUKE_TIDSLINJE_I_STEDET_FOR = "familie-ba-sak.tidslinje-opphorsperiode"

        // Release
        const val EØS_INFORMASJON_OM_ÅRLIG_KONTROLL = "familie-ba-sak.eos-informasjon-om-aarlig-kontroll"
        const val ER_MANUEL_POSTERING_TOGGLE_PÅ = "familie-ba-sak.manuell-postering"
        const val VEDTAKSPERIODE_NY = "familie-ba-sak.vedtaksperiode-ny"
        const val KAN_MIGRERE_ENSLIG_MINDREÅRIG = "familie-ba-sak.migrer-enslig-mindreaarig"

        // unleash toggles for satsendring, kan slettes etter at satsendring er skrudd på for alle satstyper
        const val SATSENDRING_ENABLET: String = "familie-ba-sak.satsendring-enablet"
        const val SATSENDRING_OPPRETT_TASKER = "familie-ba-sak.satsendring-opprett-satsendring-task"
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
