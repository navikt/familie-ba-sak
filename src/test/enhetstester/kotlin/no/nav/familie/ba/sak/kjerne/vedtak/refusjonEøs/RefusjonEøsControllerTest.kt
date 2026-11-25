package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.ba.sak.ekstern.restDomene.RefusjonEøsDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
            refusjonEøsDto.copy(id = 2L, refusjonsbeløp = 700),
            refusjonEøsDto.copy(id = 3L, refusjonsbeløp = 900, land = "DK"),
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        justRun { tilgangService.validerKanRedigereBehandling(behandlingId) }
    }

    @Test
    fun `skal returnere oppdaterte perioder etter oppretting`() {
        // Arrange
        every { refusjonEøsService.leggTilRefusjonEøsPeriode(refusjonEøsDto, behandlingId) } returns 2L
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns lagredePerioder

        // Act
        val respons = refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId, refusjonEøsDto)

        // Assert
        assertThat(respons.body?.data).isEqualTo(lagredePerioder)
    }

    @Test
    fun `skal returnere oppdaterte perioder etter oppdatering`() {
        // Arrange
        justRun { refusjonEøsService.oppdaterRefusjonEøsPeriode(refusjonEøsDto, 2L) }
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns lagredePerioder

        // Act
        val respons = refusjonEøsController.oppdaterRefusjonEøsPeriode(behandlingId, 2L, refusjonEøsDto)

        // Assert
        assertThat(respons.body?.data).isEqualTo(lagredePerioder)
    }

    @Test
    fun `skal returnere gjenværende perioder etter sletting`() {
        // Arrange
        val gjenværendePerioder = lagredePerioder.drop(1)
        justRun { refusjonEøsService.fjernRefusjonEøsPeriode(2L) }
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns gjenværendePerioder

        // Act
        val respons = refusjonEøsController.fjernRefusjonEøsPeriode(behandlingId, 2L)

        // Assert
        assertThat(respons.body?.data).isEqualTo(gjenværendePerioder)
    }
}
