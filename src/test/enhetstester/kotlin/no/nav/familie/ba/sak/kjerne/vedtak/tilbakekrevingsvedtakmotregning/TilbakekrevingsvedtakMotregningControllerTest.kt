package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.OppdaterTilbakekrevingsvedtakMotregningDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    private val oppdaterTilbakekrevingsvedtakMotregningDto =
        OppdaterTilbakekrevingsvedtakMotregningDto(
            årsakTilFeilutbetaling = null,
            vurderingAvSkyld = null,
            varselDato = null,
            samtykke = null,
            heleBeløpetSkalKrevesTilbake = null,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
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
                oppdaterTilbakekrevingsvedtakMotregningDto = oppdaterTilbakekrevingsvedtakMotregningDto,
            )

            // Assert
            verify(exactly = 1) {
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
                    handling = "Hent tilbakekrevingsvedtak motregning",
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

        @Test
        fun `skal validere tilgang til behandlingen på alle endepunkter`() {
            // Act
            tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.slettTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregningPdf(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.opprettOgHentTilbakekrevingsvedtakMotregningPdf(behandlingId = behandling.id)
            tilbakekrevingsvedtakMotregningController.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                oppdaterTilbakekrevingsvedtakMotregningDto = oppdaterTilbakekrevingsvedtakMotregningDto,
            )

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.DELETE) }
            verify(exactly = 2) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.ACCESS) }
            verify(exactly = 2) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.UPDATE) }
        }

        @Test
        fun `skal ikke hente pdf når saksbehandler mangler tilgang til behandlingen`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.ACCESS)
            } throws RolleTilgangskontrollFeil("Mangler tilgang til behandlingen")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> {
                tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregningPdf(behandlingId = behandling.id)
            }

            verify(exactly = 0) { tilbakekrevingsvedtakMotregningService.hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(any()) }
        }
    }

    @Nested
    inner class HentTilbakekrevingsvedtakMotregning {
        @Test
        fun `skal returnere tilbakekrevingsvedtak motregning på behandlingen`() {
            // Act
            val respons = tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)

            // Assert
            assertThat(respons.body?.data).isEqualTo(tilbakekrevingsvedtakMotregning.tilTilbakekrevingsvedtakMotregningDto())
        }

        @Test
        fun `skal returnere null når behandlingen ikke har tilbakekrevingsvedtak motregning`() {
            // Arrange
            every { tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandling.id) } returns null

            // Act
            val respons = tilbakekrevingsvedtakMotregningController.hentTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }
    }

    @Nested
    inner class ReturtypeTest {
        @Test
        fun oppdaterTilbakekrevingsvedtakMotregning() {
            // Act
            val response =
                tilbakekrevingsvedtakMotregningController.oppdaterTilbakekrevingsvedtakMotregning(
                    behandlingId = behandling.id,
                    oppdaterTilbakekrevingsvedtakMotregningDto = oppdaterTilbakekrevingsvedtakMotregningDto,
                )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(response.body!!.data).isEqualTo(tilbakekrevingsvedtakMotregning.tilTilbakekrevingsvedtakMotregningDto())
        }

        @Test
        fun slettTilbakekrevingsvedtakMotregning() {
            // Act
            val response =
                tilbakekrevingsvedtakMotregningController.slettTilbakekrevingsvedtakMotregning(behandlingId = behandling.id)

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(response.body!!.data).isNull()
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
