package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import org.junit.jupiter.api.Test

internal class BehandleFødselshendelseTaskTest {

    @Test
    fun `håndterer syntetisk fødselsnummer`() {
        val autovedtakStegService =
            mockk<AutovedtakStegService>().apply { every { kjørBehandlingFødselshendelse(any(), any()) } returns "" }
        settOppBehandleFødselshendelseTask(autovedtakStegService).doTask(
            BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(
                    nyBehandling = NyBehandlingHendelse(
                        morsIdent = randomFnr(),
                        barnasIdenter = listOf("61031999277")
                    )
                )
            )
        )
        verify { autovedtakStegService.kjørBehandlingFødselshendelse(any(), any()) }
    }

    @Test
    fun `håndterer vanlig fødselsnummer`() {
        val autovedtakStegService =
            mockk<AutovedtakStegService>().apply { every { kjørBehandlingFødselshendelse(any(), any()) } returns "" }
        settOppBehandleFødselshendelseTask(autovedtakStegService).doTask(
            BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(
                    nyBehandling = NyBehandlingHendelse(
                        morsIdent = randomFnr(),
                        barnasIdenter = listOf("31018721832")
                    )
                )
            )
        )
        verify { autovedtakStegService.kjørBehandlingFødselshendelse(any(), any()) }
    }

    private fun settOppBehandleFødselshendelseTask(autovedtakStegService: AutovedtakStegService): BehandleFødselshendelseTask =
        BehandleFødselshendelseTask(
            autovedtakStegService,
            mockk<VelgFagSystemService>().apply {
                every<Pair<FagsystemRegelVurdering, FagsystemUtfall>> { velgFagsystem(any()) } returns Pair(
                    FagsystemRegelVurdering.SEND_TIL_BA,
                    FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK
                )
            },
            mockk(),
            mockk<PersonidentService>().apply { every { hentAktør(any()) } returns mockk() }
        )
}
