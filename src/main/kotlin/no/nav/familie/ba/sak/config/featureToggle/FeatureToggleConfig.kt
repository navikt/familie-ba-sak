package no.nav.familie.ba.sak.config

class FeatureToggleConfig {
    companion object {
        // Release
        const val ENDRET_EØS_REGELVERKFILTER_FOR_BARN = "familie-ba-sak.endret-eos-regelverkfilter-for-barn"
        const val NY_GENERERING_AV_BREVOBJEKTER = "familie-ba-sak.ny-generering-av-brevobjekter"
        const val SATSENDRING_ENABLET: String = "familie-ba-sak.satsendring-enablet"

        // Ny utbetalingsgenerator
        const val BRUK_NY_UTBETALINGSGENERATOR = "familie.ba.sak.bruk-ny-utbetalingsgenerator"
        const val KONTROLLER_NY_UTBETALINGSGENERATOR = "familie-ba-sak.kontroller-ny-utbetalingsgenerator"

        // Operasjonelle
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"
    }
}
