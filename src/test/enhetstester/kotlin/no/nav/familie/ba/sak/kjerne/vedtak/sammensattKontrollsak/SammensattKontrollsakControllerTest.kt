package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.OpprettSammensattKontrollsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.SammensattKontrollsakDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SammensattKontrollsakControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val sammensattKontrollsakService = mockk<SammensattKontrollsakService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val sammensattKontrollsakController =
        SammensattKontrollsakController(
            tilgangService = tilgangService,
            sammensattKontrollsakService = sammensattKontrollsakService,
            utvidetBehandlingService = mockk(),
            featureToggleService = featureToggleService,
        )

    private val behandlingId = 1L
    private val sammensattKontrollsak = SammensattKontrollsak(id = 2L, behandlingId = behandlingId, fritekst = "Fritekst")
    private val sammensattKontrollsakDto = SammensattKontrollsakDto(id = sammensattKontrollsak.id, behandlingId = behandlingId, fritekst = "Fritekst")

    @BeforeEach
    fun setUp() {
        every { featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER) } returns true
        every { tilgangService.validerTilgangTilBehandling(any(), any()) } just runs
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { tilgangService.validerKanRedigereBehandling(any()) } just runs
        every { sammensattKontrollsakService.finnSammensattKontrollsak(behandlingId) } returns sammensattKontrollsak
        every { sammensattKontrollsakService.opprettSammensattKontrollsak(any()) } returns sammensattKontrollsak
        every { sammensattKontrollsakService.oppdaterSammensattKontrollsak(any()) } returns sammensattKontrollsak
        every { sammensattKontrollsakService.slettSammensattKontrollsak(any(), any()) } just runs
    }

    @Test
    fun `skal validere tilgang til behandlingen på alle endepunkter`() {
        // Act
        sammensattKontrollsakController.hentSammensattKontrollsak(behandlingId = behandlingId)
        sammensattKontrollsakController.opprettSammensattKontrollsak(OpprettSammensattKontrollsakDto(behandlingId = behandlingId, fritekst = "Fritekst"))
        sammensattKontrollsakController.oppdaterSammensattKontrollsak(sammensattKontrollsakDto)
        sammensattKontrollsakController.slettSammensattKontrollsak(sammensattKontrollsakDto)

        // Assert
        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS) }
        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.CREATE) }
        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE) }
        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE) }
        verify(exactly = 1) { sammensattKontrollsakService.slettSammensattKontrollsak(id = sammensattKontrollsak.id, behandlingId = behandlingId) }
    }

    @Test
    fun `skal ikke hente sammensatt kontrollsak når saksbehandler mangler tilgang til behandlingen`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        } throws RolleTilgangskontrollFeil("Mangler tilgang til behandlingen")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            sammensattKontrollsakController.hentSammensattKontrollsak(behandlingId = behandlingId)
        }

        verify(exactly = 0) { sammensattKontrollsakService.finnSammensattKontrollsak(any()) }
    }
}
