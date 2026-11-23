package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KorrigertVedtakControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val korrigertVedtakService = mockk<KorrigertVedtakService>()

    private val korrigertVedtakController =
        KorrigertVedtakController(
            tilgangService = tilgangService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            korrigertVedtakService = korrigertVedtakService,
        )

    private val behandling = lagBehandling()
    private val korrigertVedtak =
        KorrigertVedtak(
            id = 2L,
            vedtaksdato = LocalDate.of(2026, 1, 1),
            begrunnelse = "Feil i tidligere vedtak",
            behandling = behandling,
            aktiv = true,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentKorrigertVedtakPåBehandling {
        @Test
        fun `skal returnere aktivt korrigert vedtak på behandlingen`() {
            // Arrange
            every { korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId = behandling.id) } returns korrigertVedtak

            // Act
            val respons = korrigertVedtakController.hentKorrigertVedtakPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.data?.id).isEqualTo(korrigertVedtak.id)
        }

        @Test
        fun `skal returnere null når behandlingen ikke har aktivt korrigert vedtak`() {
            // Arrange
            every { korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId = behandling.id) } returns null

            // Act
            val respons = korrigertVedtakController.hentKorrigertVedtakPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }

        @Test
        fun `skal validere tilgang til behandlingen og kreve veilederrolle`() {
            // Arrange
            every { korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId = behandling.id) } returns null

            // Act
            korrigertVedtakController.hentKorrigertVedtakPåBehandling(behandling.id)

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilBehandling(
                    behandlingId = behandling.id,
                    event = AuditLoggerEvent.ACCESS,
                )
            }
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "Hent korrigert vedtak",
                )
            }
        }
    }
}
