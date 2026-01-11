package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.event.ContextClosedEvent

class AutovedtakSvalbardtilleggSchedulerTest {
    private val leaderClientService = mockk<LeaderClientService>()
    private val autovedtakSvalbardtilleggTaskOppretter = mockk<AutovedtakSvalbardtilleggTaskOppretter>()

    private val autovedtakSvalbardtilleggScheduler =
        AutovedtakSvalbardtilleggScheduler(
            leaderClientService = leaderClientService,
            autovedtakSvalbardtilleggTaskOppretter = autovedtakSvalbardtilleggTaskOppretter,
        )

    @Nested
    inner class KjørAutovedtakSvalbardtillegg {
        @BeforeEach
        fun setup() {
            every { leaderClientService.isLeader() } returns true
            every { autovedtakSvalbardtilleggTaskOppretter.opprettTasker(any()) } just runs
        }

        @Test
        fun `skal ikke opprette tasks når isShuttingDown er true`() {
            // Arrange
            autovedtakSvalbardtilleggScheduler.onApplicationEvent(mockk<ContextClosedEvent>())

            // Act
            autovedtakSvalbardtilleggScheduler.kjørAutovedtakSvalbardtillegg()

            // Assert
            verify(exactly = 1) { leaderClientService.isLeader() }
            verify(exactly = 0) { autovedtakSvalbardtilleggTaskOppretter.opprettTasker(any()) }
        }

        @Test
        fun `skal ikke opprette tasks når ikke leader`() {
            // Arrange
            every { leaderClientService.isLeader() } returns false

            // Act
            autovedtakSvalbardtilleggScheduler.kjørAutovedtakSvalbardtillegg()

            // Assert
            verify(exactly = 2) { leaderClientService.isLeader() }
            verify(exactly = 0) { autovedtakSvalbardtilleggTaskOppretter.opprettTasker(any()) }
        }

        @Test
        fun `skal opprette tasks`() {
            // Act
            autovedtakSvalbardtilleggScheduler.kjørAutovedtakSvalbardtillegg()

            // Assert
            verify(exactly = 2) { leaderClientService.isLeader() }
            verify(exactly = 1) { autovedtakSvalbardtilleggTaskOppretter.opprettTasker(any()) }
        }
    }
}
