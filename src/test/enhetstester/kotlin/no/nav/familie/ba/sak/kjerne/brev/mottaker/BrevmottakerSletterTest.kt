package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class BrevmottakerSletterTest {
    private val brevmottakerRepository: BrevmottakerRepository = mockk()
    private val loggService: LoggService = mockk()

    private val brevmottakerSletter =
        BrevmottakerSletter(
            brevmottakerRepository = brevmottakerRepository,
            loggService = loggService,
        )

    @Nested
    inner class SlettBrevmottaker {
        @Test
        fun `skal logge og slette brevmottaker`() {
            // Arrange
            val brevmottaker = lagBrevmottakerDb(behandlingId = 1).copy(id = 200)

            every { brevmottakerRepository.findByIdOrNull(200) } returns brevmottaker
            every { loggService.opprettBrevmottakerLogg(brevmottaker, true) } just runs
            every { brevmottakerRepository.deleteById(200) } just runs

            // Act
            brevmottakerSletter.slettBrevmottaker(id = 200, behandlingId = 1)

            // Assert
            verify(exactly = 1) { loggService.opprettBrevmottakerLogg(brevmottaker, true) }
            verify(exactly = 1) { brevmottakerRepository.deleteById(200) }
        }

        @Test
        fun `skal kaste feil og ikke slette når brevmottakeren ikke finnes`() {
            // Arrange
            every { brevmottakerRepository.findByIdOrNull(404) } returns null

            // Act & Assert
            assertThatThrownBy { brevmottakerSletter.slettBrevmottaker(id = 404, behandlingId = 1) }
                .isInstanceOf(Feil::class.java)
                .hasMessage("Finner ikke brevmottaker med id=404 på behandling med id=1")

            verify(exactly = 0) { loggService.opprettBrevmottakerLogg(any(), any()) }
            verify(exactly = 0) { brevmottakerRepository.deleteById(any()) }
        }

        @Test
        fun `skal kaste feil og ikke slette når brevmottakeren tilhører en annen behandling`() {
            // Arrange
            val brevmottaker = lagBrevmottakerDb(behandlingId = 2).copy(id = 200)

            every { brevmottakerRepository.findByIdOrNull(200) } returns brevmottaker

            // Act & Assert
            assertThatThrownBy { brevmottakerSletter.slettBrevmottaker(id = 200, behandlingId = 1) }
                .isInstanceOf(Feil::class.java)
                .hasMessage("Finner ikke brevmottaker med id=200 på behandling med id=1")

            verify(exactly = 0) { loggService.opprettBrevmottakerLogg(any(), any()) }
            verify(exactly = 0) { brevmottakerRepository.deleteById(any()) }
        }
    }
}
