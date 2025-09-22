package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.event.ContextClosedEvent

class AutovedtakFinnmarkstilleggSchedulerTest {
    private val leaderClientService = mockk<LeaderClientService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val autovedtakFinnmarkstilleggTaskOppretter = mockk<AutovedtakFinnmarkstilleggTaskOppretter>()

    private val autovedtakFinnmarkstilleggScheduler =
        AutovedtakFinnmarkstilleggScheduler(
            leaderClientService = leaderClientService,
            featureToggleService = featureToggleService,
            autovedtakFinnmarkstilleggTaskOppretter = autovedtakFinnmarkstilleggTaskOppretter,
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG) } returns true
        every { leaderClientService.isLeader() } returns true
    }

    @Nested
    inner class KjørAutovedtakFinnmarkstillegg {
        @Test
        fun `skal ikke kjøre når isShuttingDown er true`() {
            // Arrange
            autovedtakFinnmarkstilleggScheduler.onApplicationEvent(mockk<ContextClosedEvent>())

            // Act
            autovedtakFinnmarkstilleggScheduler.kjørAutovedtakFinnmarkstillegg()

            // Assert
            verify(exactly = 0) { leaderClientService.isLeader() }
            verify(exactly = 0) { autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(any()) }
        }

        @Test
        fun `skal ikke kjøre når ikke leader`() {
            // Arrange
            every { leaderClientService.isLeader() } returns false

            // Act
            autovedtakFinnmarkstilleggScheduler.kjørAutovedtakFinnmarkstillegg()

            // Assert
            verify(exactly = 1) { leaderClientService.isLeader() }
            verify(exactly = 0) { autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(any()) }
        }

        @Test
        fun `skal ikke kjøre når feature toggle er disabled`() {
            // Arrange
            every { featureToggleService.isEnabled(AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG) } returns false

            // Act
            autovedtakFinnmarkstilleggScheduler.kjørAutovedtakFinnmarkstillegg()

            // Assert
            verify(exactly = 1) { leaderClientService.isLeader() }
            verify(exactly = 0) { autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(any()) }
        }

        @Test
        fun `skal kjøre autovedtak finnmarkstillegg`() {
            // Arrange
            every { autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(any()) } just runs

            // Act
            autovedtakFinnmarkstilleggScheduler.kjørAutovedtakFinnmarkstillegg()

            // Assert
            verify(exactly = 1) { leaderClientService.isLeader() }
            verify(exactly = 1) { autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(any()) }
        }
    }
}
