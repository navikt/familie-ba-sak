package no.nav.familie.ba.sak.kjerne.småbarnstillegg

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SmåbarnstilleggControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val småbarnstilleggKorrigeringService = mockk<SmåbarnstilleggKorrigeringService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val tilbakestillBehandlingTilBehandlingsresultatService = mockk<TilbakestillBehandlingTilBehandlingsresultatService>()

    private val småbarnstilleggController =
        SmåbarnstilleggController(
            tilgangService = tilgangService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            småbarnstilleggKorrigeringService = småbarnstilleggKorrigeringService,
            utvidetBehandlingService = utvidetBehandlingService,
            tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService,
        )

    private val behandling = lagBehandling()
    private val årMåned = YearMonth.of(2025, 5)

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
        every { tilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id) } returns behandling
        every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandling.id) } returns mockk()
    }

    @Test
    fun `skal tilbakestille behandlingen til behandlingsresultat etter at småbarnstillegg er lagt til`() {
        // Arrange
        every { småbarnstilleggKorrigeringService.leggTilSmåbarnstilleggPåBehandling(årMåned, behandling) } returns emptyList()

        // Act
        småbarnstilleggController.leggTilSmåBarnstilleggPåBehandling(behandling.id, SmåbarnstilleggKorrigeringRequest(årMåned))

        // Assert
        verifyOrder {
            småbarnstilleggKorrigeringService.leggTilSmåbarnstilleggPåBehandling(årMåned, behandling)
            tilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)
        }
    }

    @Test
    fun `skal tilbakestille behandlingen til behandlingsresultat etter at småbarnstillegg er fjernet`() {
        // Arrange
        every { småbarnstilleggKorrigeringService.fjernSmåbarnstilleggPåBehandling(årMåned, behandling) } returns emptyList()

        // Act
        småbarnstilleggController.fjernSmåbarnstilleggFraMåned(behandling.id, SmåbarnstilleggKorrigeringRequest(årMåned))

        // Assert
        verifyOrder {
            småbarnstilleggKorrigeringService.fjernSmåbarnstilleggPåBehandling(årMåned, behandling)
            tilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)
        }
    }
}
