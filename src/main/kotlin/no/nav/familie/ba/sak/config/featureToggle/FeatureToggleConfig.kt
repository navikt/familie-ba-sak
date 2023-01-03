package no.nav.familie.ba.sak.config

class FeatureToggleConfig {
    companion object {
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val SKATTEETATEN_API_EKTE_DATA = "familie-ba-sak.skatteetaten-api-ekte-data-i-respons"
        const val IKKE_STOPP_MIGRERINGSBEHANDLING = "familie-ba-sak.ikke.stopp.migeringsbehandling"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"
        const val KAN_MANUELT_MIGRERE_ANNET_ENN_DELT_BOSTED = "familie-ba-sak.manuell-migrering-ikke-delt-bosted"
        const val ENDRINGER_I_VALIDERING_FOR_MIGRERINGSBEHANDLING =
            "familie-ba-sak.endringer.validering.migeringsbehandling"
        const val SJEKK_OM_UTVIDET_ER_ENDRET_BEHANDLINGSRESULTAT =
            "familie-ba-sak.behandling.behandlingsresultat-utvidet-endret"
        const val NY_MÅTE_Å_GENERERE_ATY_BARNA = "familie-ba-sak.behandling.ny-metode-generer-aty-barna"

        const val KAN_BEHANDLE_UTVIDET_EØS_SEKUNDÆRLAND = "familie-ba-sak.behandling.utvidet-eos-sekunderland"

        const val KAN_GENERERE_UTBETALINGSOPPDRAG_NY = "familie-ba-sak.generer.utbetalingsoppdrag.ny"
        const val KAN_GENERERE_UTBETALINGSOPPDRAG_NY_VALIDERING =
            "familie-ba-sak.generer.utbetalingsoppdrag.ny.validering"

        const val SKAL_KUNNE_KORRIGERE_VEDTAK = "familie-ba-sak.kunne-korrigere-vedtak"

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
