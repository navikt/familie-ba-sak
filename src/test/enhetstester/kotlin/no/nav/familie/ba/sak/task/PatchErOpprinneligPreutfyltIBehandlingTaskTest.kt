package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultatRepository
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test

class PatchErOpprinneligPreutfyltIBehandlingTaskTest {
    private val vilkårResultatRepository = mockk<VilkårResultatRepository>(relaxed = true)

    private val task =
        PatchErOpprinneligPreutfyltIBehandlingTask(
            vilkårResultatRepository = vilkårResultatRepository,
        )

    private fun lagTask(
        antall: Int,
        dryRun: Boolean,
    ) = Task(
        type = PatchErOpprinneligPreutfyltIBehandlingTask.TASK_STEP_TYPE,
        payload = jsonMapper.writeValueAsString(PatchErOpprinneligPreutfyltIBehandlingDto(antall, dryRun)),
    )

    @Test
    fun `skal oppdatere vilkårresultater som mangler erOpprinneligPreutfyltIBehandling`() {
        every { vilkårResultatRepository.finnPreutfylteVilkårResultaterUtenBehandlingId(10) } returns listOf(1L, 2L, 3L)

        task.doTask(lagTask(antall = 10, dryRun = false))

        verify(exactly = 1) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(1L) }
        verify(exactly = 1) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(2L) }
        verify(exactly = 1) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(3L) }
    }

    @Test
    fun `skal ikke oppdatere noe ved dry run`() {
        every { vilkårResultatRepository.finnPreutfylteVilkårResultaterUtenBehandlingId(10) } returns listOf(1L, 2L, 3L)

        task.doTask(lagTask(antall = 10, dryRun = true))

        verify(exactly = 0) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(any()) }
    }

    @Test
    fun `skal ikke oppdatere noe dersom ingen vilkårresultater mangler erOpprinneligPreutfyltIBehandling`() {
        every { vilkårResultatRepository.finnPreutfylteVilkårResultaterUtenBehandlingId(10) } returns emptyList()

        task.doTask(lagTask(antall = 10, dryRun = false))

        verify(exactly = 0) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(any()) }
    }

    @Test
    fun `skal begrense antall oppdateringer til antall i payload`() {
        every { vilkårResultatRepository.finnPreutfylteVilkårResultaterUtenBehandlingId(2) } returns listOf(1L, 2L)

        task.doTask(lagTask(antall = 2, dryRun = false))

        verify(exactly = 1) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(1L) }
        verify(exactly = 1) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(2L) }
        verify(exactly = 2) { vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(any()) }
    }
}
