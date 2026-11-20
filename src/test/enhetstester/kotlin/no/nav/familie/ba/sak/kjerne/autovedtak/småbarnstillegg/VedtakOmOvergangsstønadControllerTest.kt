package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.infotrygd.Personident
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtakOmOvergangsstønadControllerTest {
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val tilgangService = mockk<TilgangService>()

    private val vedtakOmOvergangsstønadController =
        VedtakOmOvergangsstønadController(
            taskRepository = taskRepository,
            tilgangService = tilgangService,
        )

    private val personident = Personident(randomFnr())

    @BeforeEach
    fun setUp() {
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { taskRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `skal kreve systemrolle for å håndtere vedtak om overgangsstønad`() {
        // Act
        vedtakOmOvergangsstønadController.håndterVedtakOmOvergangsstønad(personident)

        // Assert
        verify(exactly = 1) {
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.SYSTEM,
                handling = "håndtere vedtak om overgangsstønad",
            )
        }
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    fun `skal ikke opprette task når kaller mangler systemrolle`() {
        // Arrange
        every {
            tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SYSTEM, handling = any())
        } throws RolleTilgangskontrollFeil("Krever SYSTEM")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            vedtakOmOvergangsstønadController.håndterVedtakOmOvergangsstønad(personident)
        }

        verify(exactly = 0) { taskRepository.save(any()) }
    }
}
