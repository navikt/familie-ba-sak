package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrevmottakerOppretterTest {
    private val brevmottakerRepository: BrevmottakerRepository = mockk()
    private val validerBrevmottakerService: ValiderBrevmottakerService = mockk()
    private val loggService: LoggService = mockk()

    private val brevmottakerOppretter =
        BrevmottakerOppretter(
            brevmottakerRepository = brevmottakerRepository,
            loggService = loggService,
            validerBrevmottakerService = validerBrevmottakerService,
        )

    @Nested
    inner class OpprettBrevmottaker {
        @Test
        fun `skal validere, logge og lagre brevmottaker`() {
            // Arrange
            val brevmottaker = lagBrevmottakerDb(behandlingId = 200)
            val lagretBrevmottaker = brevmottaker.copy(id = 1)

            every {
                validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                    behandlingId = 200,
                    nyBrevmottaker = brevmottaker,
                    ekstraBarnLagtTilIBrev = emptyList(),
                )
            } just runs
            every { loggService.opprettBrevmottakerLogg(brevmottaker, false) } just runs
            every { brevmottakerRepository.save(brevmottaker) } returns lagretBrevmottaker

            // Act
            val resultat = brevmottakerOppretter.opprettBrevmottaker(brevmottaker)

            // Assert
            assertThat(resultat).isSameAs(lagretBrevmottaker)
            verify(exactly = 1) {
                validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                    behandlingId = 200,
                    nyBrevmottaker = brevmottaker,
                    ekstraBarnLagtTilIBrev = emptyList(),
                )
            }
            verify(exactly = 1) { loggService.opprettBrevmottakerLogg(brevmottaker, false) }
            verify(exactly = 1) { brevmottakerRepository.save(brevmottaker) }
        }

        @Test
        fun `skal verken logge eller lagre brevmottaker når valideringen feiler`() {
            // Arrange
            val brevmottaker = lagBrevmottakerDb(behandlingId = 200)

            every {
                validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                    any(),
                    any(),
                    any(),
                )
            } throws FunksjonellFeil("Ugyldig brevmottaker")

            // Act & Assert
            assertThatThrownBy { brevmottakerOppretter.opprettBrevmottaker(brevmottaker) }
                .isInstanceOf(FunksjonellFeil::class.java)
                .hasMessage("Ugyldig brevmottaker")

            verify(exactly = 0) { loggService.opprettBrevmottakerLogg(any(), any()) }
            verify(exactly = 0) { brevmottakerRepository.save(any()) }
        }
    }
}
