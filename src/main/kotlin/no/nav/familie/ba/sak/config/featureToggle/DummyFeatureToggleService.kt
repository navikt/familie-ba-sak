package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService

class DummyFeatureToggleService(
    private val unleash: FeatureToggleConfig.Unleash
) : FeatureToggleService {

    private val overstyrteBrytere = mapOf(
        Pair(FeatureToggleConfig.TREKK_I_LØPENDE_UTBETALING, true)
    )

    override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        if (unleash.cluster == "lokalutvikling") {
            return false
        }

        return overstyrteBrytere.getOrDefault(toggleId, false)
    }
}
