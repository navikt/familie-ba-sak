package no.nav.familie.ba.sak.config

class FeatureToggleConfig {
    companion object {
        // Release
        const val ENDRET_EØS_REGELVERKFILTER_FOR_BARN = "familie-ba-sak.endret-eos-regelverkfilter-for-barn"
        const val KAN_KJØRE_AUTOMATISK_VALUTAJUSTERING = "familie-ba-sak.automatisk-valutajustering"

        // Operasjonelle
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"
        const val HENT_IDENTER_TIL_PSYS_FRA_INFOTRYGD = "familie-ba-sak.hent-identer-til-psys-fra-infotrygd"
        const val KAN_STARTE_VALUTAJUSTERING = "familie-ba-sak.kan-starte-valutajustering"

        // satsendring
        // Oppretter satsendring-tasker for de som ikke har fått ny task
        const val SATSENDRING_ENABLET: String = "familie-ba-sak.satsendring-enablet"

        // kjører satsendring i arbeidstid med lavt eller høyt volum
        const val SATSENDRING_HØYT_VOLUM: String = "familie-ba-sak.satsendring-hoyt-volum"

        // Kjører satsendring i utenfor arbeidstid
        const val SATSENDRING_KVELD: String = "familie-ba-sak.satsendring-kveld"

        // Kjører satsendring lørdag
        const val SATSENDRING_LØRDAG: String = "familie-ba-sak.satsendring-lordag"
    }
}
