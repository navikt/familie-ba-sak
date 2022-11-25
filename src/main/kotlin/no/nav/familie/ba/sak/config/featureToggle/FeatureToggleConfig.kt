package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.config.featureToggle.DummyFeatureToggleService
import no.nav.familie.ba.sak.config.featureToggle.UnleashFeatureToggleService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import java.net.URI

@ConfigurationProperties("funksjonsbrytere")
@ConstructorBinding
class FeatureToggleConfig(
    private val enabled: Boolean,
    val unleash: Unleash
) {

    @ConstructorBinding
    data class Unleash(
        val uri: URI,
        val cluster: String,
        val applicationName: String
    )

    @Bean
    fun featureToggle(): FeatureToggleService =
        if (enabled) {
            lagUnleashFeatureToggleService()
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
            )
            lagDummyFeatureToggleService()
        }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService = UnleashFeatureToggleService(unleash)

    private fun lagDummyFeatureToggleService(): FeatureToggleService = DummyFeatureToggleService(unleash)

    companion object {
        const val KAN_DIFFERANSEBEREGNE_SØKERS_YTELSER = "familie-ba-sak.differanseberegn-sokers-ytelser"
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val SKATTEETATEN_API_EKTE_DATA = "familie-ba-sak.skatteetaten-api-ekte-data-i-respons"
        const val IKKE_STOPP_MIGRERINGSBEHANDLING = "familie-ba-sak.ikke.stopp.migeringsbehandling"
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ba-sak.behandling.teknisk-endring"
        const val KAN_MANUELT_MIGRERE_ANNET_ENN_DELT_BOSTED = "familie-ba-sak.manuell-migrering-ikke-delt-bosted"
        const val ENDRINGER_I_VALIDERING_FOR_MIGRERINGSBEHANDLING =
            "familie-ba-sak.endringer.validering.migeringsbehandling"
        const val NY_MÅTE_Å_GENERERE_ANDELER_TILKJENT_YTELSE = "familie-ba-sak.behandling.generer-andeler-med-ny-metode"
        const val NY_MÅTE_Å_SPLITTE_VEDTAKSPERIODER = "familie-ba-sak.behandling.ny-metode-for-splitt-vedtaksperioder"
        const val SJEKK_OM_UTVIDET_ER_ENDRET_BEHANDLINGSRESULTAT =
            "familie-ba-sak.behandling.behandlingsresultat-utvidet-endret"
        const val NY_MÅTE_Å_GENERERE_ATY_BARNA = "familie-ba-sak.behandling.ny-metode-generer-aty-barna"

        const val KAN_BEHANDLE_UTVIDET_EØS_SEKUNDÆRLAND = "familie-ba-sak.behandling.utvidet-eos-sekunderland"

        const val KAN_GENERERE_UTBETALINGSOPPDRAG_NY = "familie-ba-sak.generer.utbetalingsoppdrag.ny"
        const val KAN_GENERERE_UTBETALINGSOPPDRAG_NY_VALIDERING =
            "familie-ba-sak.generer.utbetalingsoppdrag.ny.validering"
        const val KAN_MIGRERE_EØS_PRIMÆRLAND_ORDINÆR = "familie-ba-sak.migrer.or-eu"
        const val KAN_MIGRERE_EØS_PRIMÆRLAND_UTVIDET = "familie-ba-sak.migrer.ut-eu"

        const val SKAL_KUNNE_KORRIGERE_VEDTAK = "familie-ba-sak.kunne-korrigere-vedtak"

        private val logger = LoggerFactory.getLogger(FeatureToggleConfig::class.java)
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
