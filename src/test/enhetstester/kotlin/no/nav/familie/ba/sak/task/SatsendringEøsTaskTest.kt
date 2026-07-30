package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class SatsendringEøsTaskTest {
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val fagsakService = mockk<FagsakService>()
    private val satsendringEøsKjøringService = mockk<SatsendringEøsKjøringService>()

    private val satsendringEøsTask =
        SatsendringEøsTask(
            autovedtakStegService = autovedtakStegService,
            fagsakService = fagsakService,
            satsendringEøsKjøringService = satsendringEøsKjøringService,
        )

    private val fagsakId = 12345L
    private val utbetalingsland = "PL"
    private val satsTidspunkt = YearMonth.of(2026, 1)
    private val aktør = randomAktør()

    private fun lagTask() =
        Task(
            type = SatsendringEøsTask.TASK_STEP_TYPE,
            payload = jsonMapper.writeValueAsString(SatsendringEøsTaskDto(fagsakId, utbetalingsland, satsTidspunkt)),
        )

    @Test
    fun `doTask skal kjøre behandling og sette ferdigTidspunkt og resultat ved suksess`() {
        // Arrange
        val task = lagTask()

        every { fagsakService.hentAktør(fagsakId) } returns aktør
        every {
            autovedtakStegService.kjørBehandlingSatsendringEøs(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                utbetalingsland = utbetalingsland,
                satsTidspunkt = satsTidspunkt,
                førstegangKjørt = any(),
            )
        } returns "Satsendring EØS kjørt OK"
        justRun { satsendringEøsKjøringService.settFerdigTidspunkt(fagsakId, utbetalingsland, satsTidspunkt) }

        // Act
        satsendringEøsTask.doTask(task)

        // Assert
        assertThat(task.metadata["resultat"]).isEqualTo("Satsendring EØS kjørt OK")
        verify(exactly = 1) { satsendringEøsKjøringService.settFerdigTidspunkt(fagsakId, utbetalingsland, satsTidspunkt) }
        verify(exactly = 0) { satsendringEøsKjøringService.settFeiltype(any(), any(), any(), any()) }
    }

    @Test
    fun `doTask skal håndtere AutovedtakSkalIkkeGjennomføresFeil uten å sette feiltype`() {
        // Arrange
        val task = lagTask()

        every { fagsakService.hentAktør(fagsakId) } returns aktør
        every {
            autovedtakStegService.kjørBehandlingSatsendringEøs(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                utbetalingsland = utbetalingsland,
                satsTidspunkt = satsTidspunkt,
                førstegangKjørt = any(),
            )
        } throws AutovedtakSkalIkkeGjennomføresFeil("Ingen endring")
        justRun { satsendringEøsKjøringService.settFerdigTidspunkt(fagsakId, utbetalingsland, satsTidspunkt) }

        // Act
        assertThatCode { satsendringEøsTask.doTask(task) }.doesNotThrowAnyException()

        // Assert
        assertThat(task.metadata["resultat"]).isEqualTo("Ingen endring")
        verify(exactly = 1) { satsendringEøsKjøringService.settFerdigTidspunkt(fagsakId, utbetalingsland, satsTidspunkt) }
        verify(exactly = 0) { satsendringEøsKjøringService.settFeiltype(any(), any(), any(), any()) }
    }

    @Test
    fun `doTask skal sette feiltype og ferdigTidspunkt ved AutovedtakMåBehandlesManueltFeil`() {
        // Arrange
        val task = lagTask()

        every { fagsakService.hentAktør(fagsakId) } returns aktør
        every {
            autovedtakStegService.kjørBehandlingSatsendringEøs(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                utbetalingsland = utbetalingsland,
                satsTidspunkt = satsTidspunkt,
                førstegangKjørt = any(),
            )
        } throws AutovedtakMåBehandlesManueltFeil("Må behandles manuelt")
        justRun { satsendringEøsKjøringService.settFeiltype(fagsakId, utbetalingsland, satsTidspunkt, "Må behandles manuelt") }
        justRun { satsendringEøsKjøringService.settFerdigTidspunkt(fagsakId, utbetalingsland, satsTidspunkt) }

        // Act
        assertThatCode { satsendringEøsTask.doTask(task) }.doesNotThrowAnyException()

        // Assert
        assertThat(task.metadata["resultat"]).isEqualTo("Må behandles manuelt")
        verify(exactly = 1) {
            satsendringEøsKjøringService.settFeiltype(fagsakId, utbetalingsland, satsTidspunkt, "Må behandles manuelt")
        }
        verify(exactly = 1) { satsendringEøsKjøringService.settFerdigTidspunkt(fagsakId, utbetalingsland, satsTidspunkt) }
    }

    @Test
    fun `doTask skal propagere uventede exceptions og ikke sette ferdigTidspunkt`() {
        // Arrange
        val task = lagTask()

        every { fagsakService.hentAktør(fagsakId) } returns aktør
        every {
            autovedtakStegService.kjørBehandlingSatsendringEøs(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                utbetalingsland = utbetalingsland,
                satsTidspunkt = satsTidspunkt,
                førstegangKjørt = any(),
            )
        } throws RuntimeException("Uventet feil")

        // Act & Assert
        assertThatThrownBy { satsendringEøsTask.doTask(task) }.isInstanceOf(RuntimeException::class.java)

        verify(exactly = 0) { satsendringEøsKjøringService.settFerdigTidspunkt(any(), any(), any()) }
        verify(exactly = 0) { satsendringEøsKjøringService.settFeiltype(any(), any(), any(), any()) }
    }
}
