package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.unleash.UnleashService

class FakeFeatureToggleService(
    unleashService: UnleashService,
    behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) : FeatureToggleService(
        unleashService = unleashService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        arbeidsfordelingPåBehandlingRepository = arbeidsfordelingPåBehandlingRepository,
    ) {
    override fun isEnabled(toggleId: String): Boolean {
        val mockUnleashServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true
        return System.getProperty(toggleId)?.toBoolean() ?: mockUnleashServiceAnswer
    }

    override fun isEnabled(
        toggle: FeatureToggle,
    ): Boolean = isEnabled(toggle.navn)

    override fun isEnabled(
        toggle: FeatureToggle,
        defaultValue: Boolean,
    ): Boolean = isEnabled(toggle.navn)

    override fun isEnabled(
        toggle: FeatureToggle,
        behandlingId: Long,
    ): Boolean = isEnabled(toggle.navn)
}
