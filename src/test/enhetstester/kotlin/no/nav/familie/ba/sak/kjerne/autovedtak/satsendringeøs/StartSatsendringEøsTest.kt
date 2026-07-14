package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSats
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatserRegister
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class StartSatsendringEøsTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val opprettTaskService = mockk<OpprettTaskService>(relaxed = true)

    private val startSatsendringEøs =
        StartSatsendringEøs(
            behandlingRepository = behandlingRepository,
            opprettTaskService = opprettTaskService,
        )

    private val land = "PL"
    private val satsTidspunkt = YearMonth.of(2025, 3)
    private val satsFom = YearMonth.of(2025, 1)

    private val sats =
        EøsSats(
            land = land,
            valuta = "PLN",
            beløp = BigDecimal("1000"),
            fom = satsFom,
            intervall = Intervall.MÅNEDLIG,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(EøsSatserRegister)
        every { EøsSatserRegister.hentSatsForLandIMåned(land, satsTidspunkt) } returns sats
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(EøsSatserRegister)
    }

    @Test
    fun `skal opprette EØS-satsendringstask for hver relevante fagsak basert på satsens fom`() {
        // Arrange
        every {
            behandlingRepository.finnLøpendeEøsFagsakerMedUtenlandskPeriodebeløpSomOverlapperSats(land, satsFom.atDay(1), null)
        } returns listOf(1L, 2L)

        // Act
        val fagsakIder = startSatsendringEøs.opprettSatsendringEøsTaskerForRelevanteFagsaker(land, satsTidspunkt)

        // Assert
        assertThat(fagsakIder).containsExactly(1L, 2L)
        verify(exactly = 1) { opprettTaskService.opprettSatsendringEøsTask(1L, land, satsTidspunkt) }
        verify(exactly = 1) { opprettTaskService.opprettSatsendringEøsTask(2L, land, satsTidspunkt) }
    }

    @Test
    fun `skal ikke opprette tasker når ingen fagsaker er relevante`() {
        // Arrange
        every {
            behandlingRepository.finnLøpendeEøsFagsakerMedUtenlandskPeriodebeløpSomOverlapperSats(land, satsFom.atDay(1), null)
        } returns emptyList()

        // Act
        val fagsakIder = startSatsendringEøs.opprettSatsendringEøsTaskerForRelevanteFagsaker(land, satsTidspunkt)

        // Assert
        assertThat(fagsakIder).isEmpty()
        verify(exactly = 0) { opprettTaskService.opprettSatsendringEøsTask(any(), any(), any()) }
    }

    @Test
    fun `skal opprette EØS-satsendringstask for én fagsak`() {
        // Act
        startSatsendringEøs.opprettSatsendringEøsTaskForFagsak(42L, land, satsTidspunkt)

        // Assert
        verify(exactly = 1) { opprettTaskService.opprettSatsendringEøsTask(42L, land, satsTidspunkt) }
    }
}
