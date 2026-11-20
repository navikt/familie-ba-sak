package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RefusjonEøsDto
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RefusjonEøsControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val refusjonEøsService = mockk<RefusjonEøsService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()

    private val refusjonEøsController =
        RefusjonEøsController(
            tilgangService = tilgangService,
            refusjonEøsService = refusjonEøsService,
            utvidetBehandlingService = utvidetBehandlingService,
        )

    private val behandlingId = 1L
    private val periodeId = 2L
    private val refusjonEøsDto =
        RefusjonEøsDto(
            id = periodeId,
            fom = LocalDate.of(2020, 1, 1),
            tom = LocalDate.of(2020, 12, 31),
            refusjonsbeløp = 100,
            land = "SE",
            refusjonAvklart = true,
        )

    @BeforeEach
    fun setUp() {
        every { tilgangService.validerTilgangTilBehandling(any(), any()) } just runs
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { tilgangService.validerKanRedigereBehandling(any()) } just runs
        every { refusjonEøsService.leggTilRefusjonEøsPeriode(any(), any()) } returns periodeId
        every { refusjonEøsService.oppdaterRefusjonEøsPeriode(any(), any(), any()) } just runs
        every { refusjonEøsService.fjernRefusjonEøsPeriode(any(), any()) } just runs
        every { refusjonEøsService.hentRefusjonEøsPerioder(any()) } returns emptyList()
        every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId) } returns mockk()
    }

    @Test
    fun `skal validere tilgang til behandlingen på alle endepunkter`() {
        // Act
        refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId = behandlingId, refusjonEøs = refusjonEøsDto)
        refusjonEøsController.oppdaterRefusjonEøsPeriode(behandlingId = behandlingId, id = periodeId, refusjonEøs = refusjonEøsDto)
        refusjonEøsController.fjernRefusjonEøsPeriode(behandlingId = behandlingId, id = periodeId)
        refusjonEøsController.hentRefusjonEøsPerioder(behandlingId = behandlingId)

        // Assert
        verify(exactly = 2) { tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE) }
        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE) }
        verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS) }
    }

    @Test
    fun `skal sende behandlingId videre slik at perioden kan knyttes til behandlingen`() {
        // Act
        refusjonEøsController.oppdaterRefusjonEøsPeriode(behandlingId = behandlingId, id = periodeId, refusjonEøs = refusjonEøsDto)
        refusjonEøsController.fjernRefusjonEøsPeriode(behandlingId = behandlingId, id = periodeId)

        // Assert
        verify(exactly = 1) { refusjonEøsService.oppdaterRefusjonEøsPeriode(refusjonEøsDto = refusjonEøsDto, id = periodeId, behandlingId = behandlingId) }
        verify(exactly = 1) { refusjonEøsService.fjernRefusjonEøsPeriode(id = periodeId, behandlingId = behandlingId) }
    }

    @Test
    fun `skal ikke legge til periode eller returnere behandlingen når saksbehandler mangler tilgang`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        } throws RolleTilgangskontrollFeil("Mangler tilgang til behandlingen")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId = behandlingId, refusjonEøs = refusjonEøsDto)
        }

        verify(exactly = 0) { refusjonEøsService.leggTilRefusjonEøsPeriode(any(), any()) }
        verify(exactly = 0) { utvidetBehandlingService.lagUtvidetBehandlingDto(any()) }
    }
}
