package no.nav.familie.ba.sak.config

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.UnleashContext
import no.finn.unleash.UnleashContextProvider
import no.finn.unleash.strategy.GradualRolloutRandomStrategy
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
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
        if (enabled)
            lagUnleashFeatureToggleService()
        else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
            )
            lagDummyFeatureToggleService()
        }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService {
        val defaultUnleash = DefaultUnleash(
            UnleashConfig.builder()
                .appName(unleash.applicationName)
                .unleashAPI(unleash.uri)
                .unleashContextProvider(lagUnleashContextProvider())
                .build(),
            ByClusterStrategy(unleash.cluster),
            ByAnsvarligSaksbehandler(),
            GradualRolloutRandomStrategy()
        )

        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return defaultUnleash.isEnabled(toggleId, defaultValue)
            }
        }
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                .appName(unleash.applicationName)
                .build()
        }
    }

    class ByClusterStrategy(private val clusterName: String) : Strategy {

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
            if (parameters.isEmpty()) return false
            return parameters["cluster"]?.contains(clusterName) ?: false
        }

        override fun getName(): String = "byCluster"
    }

    class ByAnsvarligSaksbehandler : Strategy {

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
            if (parameters.isEmpty()) return false

            return parameters["saksbehandler"]?.contains(SikkerhetContext.hentSaksbehandlerEpost()) ?: false
        }

        override fun getName(): String = "byAnsvarligSaksbehandler"
    }

    private fun lagDummyFeatureToggleService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                if (unleash.cluster == "lokalutvikling") {
                    return false
                }

                return defaultValue
            }
        }
    }

    companion object {
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ba-sak.behandling.korreksjon-vedtaksbrev"
        const val SKATTEETATEN_API_EKTE_DATA = "familie-ba-sak.skatteetaten-api-ekte-data-i-respons"
        const val KAN_BEHANDLE_EØS = "familie-ba-sak.behandling.eos"
        const val IKKE_STOPP_MIGRERINGSBEHANDLING = "familie-ba-sak.ikke.stopp.migeringsbehandling"
        const val INGEN_OVERLAPP_VEDTAKSPERIODER = "familie-ba-sak.ingen-overlapp-vedtaksperioder.utgivelse"
        const val ENDRE_MOTTAKER_ENDRINGSÅRSAKER =
            "familie-ba-sak.behandling.endringsperiode.endre-mottaker-aarsaker.utgivelse"
        const val FØRSTE_ENDRINGSTIDSPUNKT = "familie-ba-sak.behandling.forste-endringstidspunkt.utgivelse"
        const val ETTERBETALING_3ÅR = "familie-ba-sak.utgivelse.behandling.etterbetaling-3-aar"
        const val NY_DELT_BOSTED_BEGRUNNELSE = "familie-ba-sak.utgivelse.behandling.delt-bosted-begrunnelse-avtaletidspunkt"
        const val NY_MÅTE_Å_GENERERE_UTVIDET_ANDELER = "familie-ba-sak.utgivelse.behandling.generer-utvidet-andeler"

        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ba-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"

        const val TEKNISK_IVERKSETT_MOT_OPPDRAG_ALLEREDE_SENDT =
            "familie-ba-sak.teknisk-iverksett-mot-oppdrag-allerede-sendt"
        const val KONSISTENSAVSTEMMING_SPLITT_BATCH = "familie-ba-sak.teknisk-konsistensavstemming-splitt-batch"

        const val LAG_REDUKSJONSPERIODER_FRA_INNVILGELSESTIDSPUNKT =
            "familie-ba-sak.lag.reduksjonsperioder.fra.innvilgelsestidspunkt"

        const val SKAL_MIGRERE_FOSTERBARN = "familie-ba-sak.migrer-fosterbarn"

        const val SKAL_MIGRERE_ORDINÆR_DELT_BOSTED = "familie-ba-sak.migrer-delt-bosted"

        const val SKAL_MIGRERE_UTVIDET_DELT_BOSTED = "familie-ba-sak.migrer-utvidet-delt-bosted"

        const val KAN_MANUELT_MIGRERE_ANNET_ENN_DELT_BOSTED = "familie-ba-sak.manuell-migrering-ikke-delt-bosted"

        private val logger = LoggerFactory.getLogger(FeatureToggleConfig::class.java)
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
