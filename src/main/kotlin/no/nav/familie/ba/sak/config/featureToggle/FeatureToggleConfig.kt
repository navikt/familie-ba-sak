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
        const val FEILUTBETALT_VALUTA_PR_MND = "familie-ba-sak.feilutbetalt-valuta-pr-mnd"
        const val SATSENDRING_KOPIER_GRUNNLAG_FRA_FORRIGE_BEHANDLING = "familie-ba-sak.satsendring.kopier-grunnlag-fra-forrige-behandling"
        const val BEGRUNNELSER_NY = "familie-ba-sak.begrunnelser-ny"
        const val ENDRINGSTIDSPUNKT = "familie-ba-sak.endringstidspunkt"

        // unleash toggles for satsendring, kan slettes etter at satsendring er skrudd på for alle satstyper
        const val SATSENDRING_ENABLET: String = "familie-ba-sak.satsendring-enablet"
        const val SATSENDRING_SNIKE_I_KØEN = "familie-ba-sak.satsendring-snike-i-koen"
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
