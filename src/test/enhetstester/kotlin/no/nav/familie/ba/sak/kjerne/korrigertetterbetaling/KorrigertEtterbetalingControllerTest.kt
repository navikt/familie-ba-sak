package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KorrigertEtterbetalingControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val korrigertEtterbetalingService = mockk<KorrigertEtterbetalingService>()

    private val korrigertEtterbetalingController =
        KorrigertEtterbetalingController(
            tilgangService = tilgangService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            korrigertEtterbetalingService = korrigertEtterbetalingService,
        )

    private val behandling = lagBehandling()
    private val korrigertEtterbetaling =
        lagKorrigertEtterbetaling(
            behandling = behandling,
            begrunnelse = "Motregnet mot feilutbetaling",
            beløp = 2500,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentKorrigertEtterbetalingPåBehandling {
        @Test
        fun `skal returnere aktiv korrigert etterbetaling på behandlingen`() {
            // Arrange
            every { korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandlingId = behandling.id) } returns korrigertEtterbetaling

            // Act
            val respons = korrigertEtterbetalingController.hentKorrigertEtterbetalingPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.data?.beløp).isEqualTo(korrigertEtterbetaling.beløp)
            assertThat(respons.body?.data?.begrunnelse).isEqualTo(korrigertEtterbetaling.begrunnelse)
        }

        @Test
        fun `skal returnere null når behandlingen ikke har aktiv korrigert etterbetaling`() {
            // Arrange
            every { korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandlingId = behandling.id) } returns null

            // Act
            val respons = korrigertEtterbetalingController.hentKorrigertEtterbetalingPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }
    }
}
