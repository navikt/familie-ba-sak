package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagFiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FiltreringResultatControllerTest {
    private val filtreringResultatRepository = mockk<FiltreringResultatRepository>()
    private val tilgangService = mockk<TilgangService>()

    private val filtreringResultatController =
        FiltreringResultatController(
            filtreringResultatRepository = filtreringResultatRepository,
            tilgangService = tilgangService,
        )

    private val behandlingId = 1L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentFiltreringsresultater {
        @Test
        fun `skal validere tilgang til behandling`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilBehandling(
                    behandlingId = behandlingId,
                    event = AuditLoggerEvent.ACCESS,
                )
            } throws RolleTilgangskontrollFeil("Ikke tilgang")

            // Act
            val feil =
                assertThrows<RolleTilgangskontrollFeil> {
                    filtreringResultatController.hentFiltreringsresultater(behandlingId)
                }

            // Assert
            assertThat(feil.message).isEqualTo("Ikke tilgang")
            verify(exactly = 0) { filtreringResultatRepository.finnFiltreringResultater(any()) }
        }

        @Test
        fun `skal kreve minimum veilederrolle`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "Henter filtreringsresultater",
                )
            } throws RolleTilgangskontrollFeil("Ikke tilgang til handling")

            // Act
            val feil =
                assertThrows<RolleTilgangskontrollFeil> {
                    filtreringResultatController.hentFiltreringsresultater(behandlingId)
                }

            // Assert
            assertThat(feil.message).isEqualTo("Ikke tilgang til handling")
            verify(exactly = 0) { filtreringResultatRepository.finnFiltreringResultater(any()) }
        }

        @Test
        fun `skal returnere filtreringsresultatene for behandlingen`() {
            // Arrange
            val filtreringResultat =
                lagFiltreringResultat(
                    behandlingId = behandlingId,
                    filtreringsregel = Filtreringsregel.Identifikator.MOR_LEVER,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "Mor lever",
                )

            every { filtreringResultatRepository.finnFiltreringResultater(behandlingId = behandlingId) } returns listOf(filtreringResultat)

            // Act
            val respons = filtreringResultatController.hentFiltreringsresultater(behandlingId)

            // Assert
            val filtreringResultatDtoer = respons.body?.data
            assertThat(filtreringResultatDtoer).hasSize(1)
            assertThat(filtreringResultatDtoer?.single()?.filtreringsregel).isEqualTo(Filtreringsregel.Identifikator.MOR_LEVER)
            assertThat(filtreringResultatDtoer?.single()?.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(filtreringResultatDtoer?.single()?.begrunnelse).isEqualTo("Mor lever")
        }

        @Test
        fun `skal returnere tom liste når behandlingen ikke har filtreringsresultater`() {
            // Arrange
            every { filtreringResultatRepository.finnFiltreringResultater(behandlingId = behandlingId) } returns emptyList()

            // Act
            val respons = filtreringResultatController.hentFiltreringsresultater(behandlingId)

            // Assert
            assertThat(respons.body?.data).isEmpty()
        }
    }
}
