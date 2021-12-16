package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test

internal class AutobrevTaskTest {

    val fagsakRepository = mockk<FagsakRepository>()
    val opprettTaskService = mockk<OpprettTaskService>()

    private val autobrevTask = AutobrevTask(
        fagsakRepository = fagsakRepository,
        opprettTaskService = opprettTaskService
    )

    private val autoBrevTask = Task(
        type = AutobrevTask.TASK_STEP_TYPE,
        payload = ""
    )

    @Test
    fun `oppretter 2 SendAutobrev tasker for 6 år og 2 for 18 år`() {

        val fagsaker = setOf(
            Fagsak(1, aktør = tilAktør(randomFnr())),
            Fagsak(2, aktør = tilAktør(randomFnr())),
        )

        every { fagsakRepository.finnLøpendeFagsakMedBarnMedFødselsdatoInnenfor(any(), any()) } answers { fagsaker }
        every { opprettTaskService.opprettAutovedtakFor6Og18ÅrBarn(any(), any()) } just runs

        autobrevTask.doTask(autoBrevTask)

        verify(exactly = 2) {
            opprettTaskService.opprettAutovedtakFor6Og18ÅrBarn(any(), 6)
        }
        verify(exactly = 2) {
            opprettTaskService.opprettAutovedtakFor6Og18ÅrBarn(any(), 18)
        }
    }
}
