package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestOppdaterTilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate

class TilbakekrevingsvedtakMotregningControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val tilbakekrevingsvedtakMotregningService = mockk<TilbakekrevingsvedtakMotregningService>()
    private val tilbakekrevingsvedtakMotregningBrevService = mockk<TilbakekrevingsvedtakMotregningBrevService>()

    private val tilbakekrevingsvedtakMotregningController =
        TilbakekrevingsvedtakMotregningController(
            tilgangService = tilgangService,
            tilbakekrevingsvedtakMotregningService = tilbakekrevingsvedtakMotregningService,
            tilbakekrevingsvedtakMotregningBrevService = tilbakekrevingsvedtakMotregningBrevService,
        )

    private val behandling = lagBehandling()

    private val tilbakekrevingsvedtakMotregning =
        TilbakekrevingsvedtakMotregning(
            id = 0,
            behandling = behandling,
            samtykke = true,
            årsakTilFeilutbetaling = "ny årsak",
            vurderingAvSkyld = "ny vurdering",
            varselDato = LocalDate.now(),
            heleBeløpetSkalKrevesTilbake = true,
            vedtakPdf = ByteArray(0),
        )

    private val restOppdaterTilbakekrevingsvedtakMotregning =
        RestOppdaterTilbakekrevingsvedtakMotregning(
            årsakTilFeilutbetaling = null,
            vurderingAvSkyld = null,
            varselDato = null,
            samtykke = null,
            heleBeløpetSkalKrevesTilbake = null,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        justRun { tilgangService.validerKanRedigereBehandling(behandling.id) }
        every { tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandling.id) } returns tilbakekrevingsvedtakMotregning
        every { tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(behandling.id) } returns tilbakekrevingsvedtakMotregning
        every { tilbakekrevingsvedtakMotregningService.slettTilbakekrevingsvedtakMotregning(behandling.id) } returns null
        every { tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(behandling.id) } returns tilbakekrevingsvedtakMotregning
        every {
            tilbakekrevingsvedtakMotregningService.hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(
                behandling.id,
            )
        } returns tilbakekrevingsvedtakMotregning
    }

    @Nested
    inner class TilgangServiceTest {
        @Test
        fun `alle endepunkter verifiserer tilgang til handling`() {
            // Act
            tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.slettTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregningPdf(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.opprettOgHentTilbakekrevingsvedtakMotregningPdf(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                restOppdaterTilbakekrevingsvedtakMotregning = restOppdaterTilbakekrevingsvedtakMotregning,
            )

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "Hent TilbakekrevingsvedtakMotregning",
                )
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "Oppdater tilbakekrevingsvedtak motregning",
                )
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "Slett TilbakekrevingsvedtakMotregning",
                )
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "Hent TilbakekrevingsvedtakMotregning pdf",
                )
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "Oppretter TilbakekrevingsvedtakMotregning pdf",
                )
            }
        }
    }

    @Nested
    inner class ReturtypeTest {
        @Test
        fun hentTilbakekrevingsvedtakMotregning() {
            // Act
            val response =
                tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(response.body!!.data).isEqualTo(tilbakekrevingsvedtakMotregning.tilRestTilbakekrevingsvedtakMotregning())
        }

        @Test
        fun oppdaterTilbakekrevingsvedtakMotregning() {
            // Act
            val response =
                tilbakekrevingsvedtakMotregningController.oppdaterTilbakekrevingsvedtakMotregning(
                    behandlingId = behandling.id,
                    restOppdaterTilbakekrevingsvedtakMotregning = restOppdaterTilbakekrevingsvedtakMotregning,
                )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(response.body!!.data).isEqualTo(tilbakekrevingsvedtakMotregning.tilRestTilbakekrevingsvedtakMotregning())
        }

        @Test
        fun slettTilbakekrevingsvedtakMotregning() {
            // Act
            val response =
                tilbakekrevingsvedtakMotregningController.slettTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(response.body!!.data).isEqualTo("TilbakekrevingsvedtakMotregning for behandling=${behandling.id} slettet OK.")
        }

        @Test
        fun hentTilbakekrevingsvedtakMotregningPdf() {
            // Act
            val ressurs =
                tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregningPdf(behandlingId = behandling.id)

            // Assert
            assertThat(ressurs.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(ressurs.data).isEqualTo(ByteArray(0))
        }

        @Test
        fun opprettOgHentTilbakekrevingsvedtakMotregningPdf() {
            // Act
            val ressurs =
                tilbakekrevingsvedtakMotregningController.opprettOgHentTilbakekrevingsvedtakMotregningPdf(behandlingId = behandling.id)

            // Assert
            assertThat(ressurs.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(ressurs.data).isEqualTo(ByteArray(0))
        }
    }
}
