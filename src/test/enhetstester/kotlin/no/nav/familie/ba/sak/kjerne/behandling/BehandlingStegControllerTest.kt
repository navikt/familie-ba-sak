package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatStegValideringService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class BehandlingStegControllerTest {
    private val featureToggleService = mockk<FeatureToggleService>()
    private val tilgangServiceMock = mockk<TilgangService>()
    private val behandlingHentOgPersisterServiceMock = mockk<BehandlingHentOgPersisterService>()
    private val stegServiceMock = mockk<StegService>()
    private val utvidetBehandlingServiceMock = mockk<UtvidetBehandlingService>()
    private val totrinnskontrollServiceMock = mockk<TotrinnskontrollService>()
    private val behandlingsresultatStegValideringService = mockk<BehandlingsresultatStegValideringService>()
    private val behandlingStegController =
        BehandlingStegController(
            behandlingHentOgPersisterService = behandlingHentOgPersisterServiceMock,
            stegService = stegServiceMock,
            tilgangService = tilgangServiceMock,
            featureToggleService = featureToggleService,
            utvidetBehandlingService = utvidetBehandlingServiceMock,
            totrinnskontrollService = totrinnskontrollServiceMock,
            behandlingsresultatStegValideringService = behandlingsresultatStegValideringService,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `Skal kaste feil hvis saksbehandler uten teknisk endring-tilgang prøver å henlegge en behandling med årsak=teknisk endring`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)

        every { featureToggleService.isEnabled(FeatureToggle.TEKNISK_ENDRING, behandling.id) } returns false
        every {
            featureToggleService.isEnabled(
                FeatureToggle.TEKNISK_VEDLIKEHOLD_HENLEGGELSE,
                behandling.id,
            )
        } returns false
        every { tilgangServiceMock.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling
        every { stegServiceMock.håndterHenleggBehandling(any(), any()) } returns behandling

        assertThrows<FunksjonellFeil> {
            behandlingStegController.henleggBehandlingOgSendBrev(
                behandlingId = behandling.id,
                henleggInfo =
                    HenleggBehandlingInfoDto(
                        begrunnelse = "dette er en begrunnelse",
                        årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                    ),
            )
        }
    }

    @Test
    fun `Skal validere behandlingsresultat med valideringstjenesten`() {
        // Arrange
        val behandling = lagBehandling()
        every { tilgangServiceMock.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { behandlingHentOgPersisterServiceMock.hent(behandling.id) } returns behandling
        every { behandlingsresultatStegValideringService.validerBehandlingFørBehandlingsresultat(behandling) } just runs

        // Act
        val respons = behandlingStegController.validerBehandlingsresultat(behandling.id)

        // Assert
        assertThat(respons.body?.data).isTrue()
        verify(exactly = 1) {
            behandlingsresultatStegValideringService.validerBehandlingFørBehandlingsresultat(behandling)
        }
    }

    @Test
    fun `Saksbehandler kan underkjenne eget vedtak uten beslutter-rolle`() {
        // Arrange
        val behandling = lagBehandling()
        val saksbehandlerId = "Z123456"
        val totrinnskontroll = mockk<Totrinnskontroll>()

        every { SikkerhetContext.hentSaksbehandler() } returns saksbehandlerId
        every { totrinnskontroll.saksbehandlerId } returns saksbehandlerId
        every { totrinnskontrollServiceMock.hentAktivForBehandling(behandling.id) } returns totrinnskontroll
        every { tilgangServiceMock.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { behandlingHentOgPersisterServiceMock.hent(behandling.id) } returns behandling
        every { stegServiceMock.håndterBeslutningForVedtak(any(), any()) } returns behandling
        every { utvidetBehandlingServiceMock.lagUtvidetBehandlingDto(behandlingId = behandling.id) } returns mockk()

        // Act
        behandlingStegController.iverksettVedtak(
            behandlingId = behandling.id,
            beslutningPåVedtakDto = BeslutningPåVedtakDto(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Trenger mer info"),
        )

        // Assert
        verify(exactly = 1) {
            tilgangServiceMock.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = "underkjenne eget vedtak",
            )
        }
    }

    @Test
    fun `Saksbehandler kan ikke underkjenne andres vedtak uten beslutter-rolle`() {
        // Arrange
        val behandling = lagBehandling()
        val totrinnskontroll = mockk<Totrinnskontroll>()

        every { SikkerhetContext.hentSaksbehandler() } returns "Z654321"
        every { totrinnskontroll.saksbehandlerId } returns "Z123456"
        every { totrinnskontrollServiceMock.hentAktivForBehandling(behandling.id) } returns totrinnskontroll
        every { tilgangServiceMock.verifiserHarTilgangTilHandling(any(), any()) } throws
            FunksjonellFeil("Du har ikke beslutter tilgang.")

        // Act & Assert
        val feilmelding =
            assertThrows<FunksjonellFeil> {
                behandlingStegController.iverksettVedtak(
                    behandlingId = behandling.id,
                    beslutningPåVedtakDto = BeslutningPåVedtakDto(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Noe"),
                )
            }.message

        assertThat(feilmelding).isEqualTo("Du har ikke beslutter tilgang.")

        verify(exactly = 1) {
            tilgangServiceMock.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
                handling = "iverksette vedtak",
            )
        }
    }

    @Nested
    inner class RegistrerInstitusjon {
        private val mockMvc: MockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    BehandlingStegController(
                        behandlingHentOgPersisterService = behandlingHentOgPersisterServiceMock,
                        utvidetBehandlingService = mockk(relaxed = true),
                        stegService = stegServiceMock,
                        tilgangService = tilgangServiceMock,
                        featureToggleService = featureToggleService,
                        totrinnskontrollService = totrinnskontrollServiceMock,
                    ),
                ).build()

        @Test
        fun `skal ignorere body fra gammel frontend og bruke institusjonen på fagsaken`() {
            // Arrange
            val behandling = lagBehandling()
            every { behandlingHentOgPersisterServiceMock.hent(behandling.id) } returns behandling
            every { stegServiceMock.håndterRegistrerInstitusjon(behandling) } returns behandling

            // Act
            mockMvc
                .post("/api/behandlinger/${behandling.id}/steg/registrer-institusjon") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"orgNummer":"889640782","tssEksternId":"80000999999"}"""
                }.andExpect { status { isOk() } }

            // Assert
            verify(exactly = 1) { stegServiceMock.håndterRegistrerInstitusjon(behandling) }
        }

        @Test
        fun `skal fungere uten body`() {
            // Arrange
            val behandling = lagBehandling()
            every { behandlingHentOgPersisterServiceMock.hent(behandling.id) } returns behandling
            every { stegServiceMock.håndterRegistrerInstitusjon(behandling) } returns behandling

            // Act
            mockMvc
                .post("/api/behandlinger/${behandling.id}/steg/registrer-institusjon")
                .andExpect { status { isOk() } }

            // Assert
            verify(exactly = 1) { stegServiceMock.håndterRegistrerInstitusjon(behandling) }
        }
    }
}
