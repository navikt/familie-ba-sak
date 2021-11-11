package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InfotrygdFeedServiceTest : AbstractSpringIntegrationTestDev() {
    val featureToggleServiceMock = mockk<FeatureToggleService>()
    val opprettTaskServiceMock = mockk<OpprettTaskService>()

    @Test
    fun `Skal ikke send start behandling feed hvis feature er togglet av`() {
        every { featureToggleServiceMock.isEnabled(any()) } returns true
        every {
            featureToggleServiceMock.isEnabled(
                FeatureToggleConfig.SEND_START_BEHANDLING_TIL_INFOTRYGD,
                false
            )
        } returns false
        every { opprettTaskServiceMock.opprettSendStartBehandlingTilInfotrygdTask(any()) } just runs

        val infotrygdFeedService = InfotrygdFeedService(opprettTaskServiceMock, featureToggleServiceMock)
        infotrygdFeedService.sendStartBehandlingTilInfotrygdFeed("123")
        verify(exactly = 0) {
            opprettTaskServiceMock.opprettSendStartBehandlingTilInfotrygdTask(any())
        }
    }

    @Test
    fun `Skal send riktig start behandling feed hvis feature er togglet`() {
        every { featureToggleServiceMock.isEnabled(any()) } returns false
        every {
            featureToggleServiceMock.isEnabled(
                FeatureToggleConfig.SEND_START_BEHANDLING_TIL_INFOTRYGD,
                false
            )
        } returns true
        val ident = "123"
        val identSlot = slot<String>()
        every { opprettTaskServiceMock.opprettSendStartBehandlingTilInfotrygdTask(capture(identSlot)) } just runs

        val infotrygdFeedService = InfotrygdFeedService(opprettTaskServiceMock, featureToggleServiceMock)
        infotrygdFeedService.sendStartBehandlingTilInfotrygdFeed(ident)
        verify(exactly = 1) {
            opprettTaskServiceMock.opprettSendStartBehandlingTilInfotrygdTask(any())
        }
        assertThat(identSlot.captured).isEqualTo(ident)
    }
}
