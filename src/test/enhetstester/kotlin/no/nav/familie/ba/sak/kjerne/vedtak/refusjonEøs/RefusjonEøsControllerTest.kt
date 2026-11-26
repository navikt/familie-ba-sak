package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RefusjonEøsDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RefusjonEøsControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val refusjonEøsService = mockk<RefusjonEøsService>()
    private val refusjonEøsController =
        RefusjonEøsController(
            tilgangService = tilgangService,
            refusjonEøsService = refusjonEøsService,
        )

    private val behandlingId = 1L
    private val periodeId = 2L
    private val refusjonEøsDto =
        RefusjonEøsDto(
            id = null,
            fom = LocalDate.of(2026, 1, 1),
            tom = LocalDate.of(2026, 1, 31),
            refusjonsbeløp = 100,
            land = "SE",
            refusjonAvklart = true,
        )
    private val lagredePerioder =
        listOf(
            refusjonEøsDto.copy(id = periodeId, refusjonsbeløp = 700),
            refusjonEøsDto.copy(id = 3L, refusjonsbeløp = 900, land = "DK"),
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        justRun { tilgangService.validerKanRedigereBehandling(behandlingId) }
        every { refusjonEøsService.leggTilRefusjonEøsPeriode(refusjonEøsDto, behandlingId) } returns periodeId
        justRun { refusjonEøsService.oppdaterRefusjonEøsPeriode(refusjonEøsDto, periodeId, behandlingId) }
        justRun { refusjonEøsService.fjernRefusjonEøsPeriode(periodeId, behandlingId) }
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns lagredePerioder
    }

    @Test
    fun `skal returnere oppdaterte perioder etter oppretting`() {
        // Act
        val respons = refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId, refusjonEøsDto)

        // Assert
        assertThat(respons.body?.data).isEqualTo(lagredePerioder)
    }

    @Test
    fun `skal returnere oppdaterte perioder etter oppdatering`() {
        // Act
        val respons = refusjonEøsController.oppdaterRefusjonEøsPeriode(behandlingId, periodeId, refusjonEøsDto)

        // Assert
        assertThat(respons.body?.data).isEqualTo(lagredePerioder)
    }

    @Test
    fun `skal returnere gjenværende perioder etter sletting`() {
        // Arrange
        val gjenværendePerioder = lagredePerioder.drop(1)
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns gjenværendePerioder

        // Act
        val respons = refusjonEøsController.fjernRefusjonEøsPeriode(behandlingId, periodeId)

        // Assert
        assertThat(respons.body?.data).isEqualTo(gjenværendePerioder)
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
    fun `skal ikke legge til periode eller returnere perioder når saksbehandler mangler tilgang`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        } throws RolleTilgangskontrollFeil("Mangler tilgang til behandlingen")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId = behandlingId, refusjonEøs = refusjonEøsDto)
        }

        verify(exactly = 0) { refusjonEøsService.leggTilRefusjonEøsPeriode(any(), any()) }
        verify(exactly = 0) { refusjonEøsService.hentRefusjonEøsPerioder(any()) }
    }
}
