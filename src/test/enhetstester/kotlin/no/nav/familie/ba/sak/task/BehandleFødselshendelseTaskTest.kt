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
        val velgFagsystemService = mockk<VelgFagSystemService>().apply {
            every { velgFagsystem(any()) } returns Pair(
                FagsystemRegelVurdering.SEND_TIL_BA,
                FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK
            )
        }
        val personidentService = mockk<PersonidentService>().apply { every { hentAktør(any()) } returns mockk() }
        val autovedtakStegService =
            mockk<AutovedtakStegService>().apply { every { kjørBehandlingFødselshendelse(any(), any()) } returns "" }
        val service =
            BehandleFødselshendelseTask(autovedtakStegService, velgFagsystemService, mockk(), personidentService)
        val nyBehandlingHendelse = NyBehandlingHendelse(
            morsIdent = randomFnr(),
            barnasIdenter = listOf("61031999277")
        )
        service.doTask(
            BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(
                    nyBehandling = nyBehandlingHendelse
                )
            )
        )
        verify { autovedtakStegService.kjørBehandlingFødselshendelse(any(), any()) }
    }
}
