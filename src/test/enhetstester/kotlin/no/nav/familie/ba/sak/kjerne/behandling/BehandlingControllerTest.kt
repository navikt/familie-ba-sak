package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val behandlingService = mockk<BehandlingService>()

    private val behandlingController =
        BehandlingController(
            stegService = mockk(),
            behandlingHentOgPersisterService = mockk(),
            behandlingService = behandlingService,
            behandlingstemaService = mockk(),
            taskRepository = mockk(),
            tilgangService = tilgangService,
            utvidetBehandlingService = mockk(),
            tilkjentYtelseValideringService = mockk(),
        )

    private val behandlingId = 1L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentMigreringsdato {
        @Test
        fun `skal returnere migreringsdatoen på behandlingen`() {
            // Arrange
            val migreringsdato = LocalDate.of(2020, 2, 1)
            every { behandlingService.hentMigreringsdatoIBehandling(behandlingId = behandlingId) } returns migreringsdato

            // Act
            val respons = behandlingController.hentMigreringsdato(behandlingId)

            // Assert
            assertThat(respons.body?.data).isEqualTo(migreringsdato)
        }

        @Test
        fun `skal returnere null når behandlingen ikke har migreringsdato`() {
            // Arrange
            every { behandlingService.hentMigreringsdatoIBehandling(behandlingId = behandlingId) } returns null

            // Act
            val respons = behandlingController.hentMigreringsdato(behandlingId)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }
    }
}
