package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.prosessering.error.RekjørSenereException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `skal kaste rekjør senere exception hvis det opprettes satsendring task`() {
        assertThrows<RekjørSenereException> {
            BehandleFødselshendelseTask(
                autovedtakStegService = mockk<AutovedtakStegService>().apply {
                    every {
                        kjørBehandlingFødselshendelse(
                            any(),
                            any()
                        )
                    } returns ""
                },
                velgFagsystemService = mockk<VelgFagSystemService>().apply {
                    every<Pair<FagsystemRegelVurdering, FagsystemUtfall>> { velgFagsystem(any()) } returns Pair(
                        FagsystemRegelVurdering.SEND_TIL_BA,
                        FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK
                    )
                },
                infotrygdFeedService = mockk(),
                personidentService = mockk<PersonidentService>().apply { every { hentAktør(any()) } returns mockk() },
                startSatsendring = mockk<StartSatsendring>().apply {
                    every {
                        sjekkOgOpprettSatsendringVedGammelSats(
                            any<String>()
                        )
                    } returns true
                }
            ).doTask(
                BehandleFødselshendelseTask.opprettTask(
                    BehandleFødselshendelseTaskDTO(
                        nyBehandling = NyBehandlingHendelse(
                            morsIdent = randomFnr(),
                            barnasIdenter = listOf("31018721832")
                        )
                    )
                )
            )
        }
    }

    private fun settOppBehandleFødselshendelseTask(
        autovedtakStegService: AutovedtakStegService
    ): BehandleFødselshendelseTask =
        BehandleFødselshendelseTask(
            autovedtakStegService = autovedtakStegService,
            velgFagsystemService = mockk<VelgFagSystemService>().apply {
                every<Pair<FagsystemRegelVurdering, FagsystemUtfall>> { velgFagsystem(any()) } returns Pair(
                    FagsystemRegelVurdering.SEND_TIL_BA,
                    FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK
                )
            },
            infotrygdFeedService = mockk(),
            personidentService = mockk<PersonidentService>().apply { every { hentAktør(any()) } returns mockk() },
            startSatsendring = mockk<StartSatsendring>().apply {
                every {
                    sjekkOgOpprettSatsendringVedGammelSats(
                        any<String>()
                    )
                } returns false
            }
        )
}
