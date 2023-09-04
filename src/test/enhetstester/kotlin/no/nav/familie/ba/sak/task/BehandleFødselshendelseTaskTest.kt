package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
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
                        barnasIdenter = listOf("61031999277"),
                    ),
                ),
            ),
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
                        barnasIdenter = listOf("31018721832"),
                    ),
                ),
            ),
        )
        verify { autovedtakStegService.kjørBehandlingFødselshendelse(any(), any()) }
    }

    @Test
    fun `skal kaste rekjør senere exception hvis det opprettes satsendring task`() {
        assertThrows<RekjørSenereException> {
            BehandleFødselshendelseTask(
                behandlingHentOgPersisterService = mockk(),
                fagsakService = mockk(),
                oppgaveService = mockk(),
                taskRepositoryWrapper = mockk(),
                autovedtakStegService = mockk<AutovedtakStegService>().apply {
                    every {
                        kjørBehandlingFødselshendelse(
                            any(),
                            any(),
                        )
                    } returns ""
                },
                velgFagsystemService = mockk<VelgFagSystemService>().apply {
                    every<Pair<FagsystemRegelVurdering, FagsystemUtfall>> { velgFagsystem(any()) } returns Pair(
                        FagsystemRegelVurdering.SEND_TIL_BA,
                        FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                    )
                },
                infotrygdFeedService = mockk(),
                personidentService = mockk<PersonidentService>().apply { every { hentAktør(any()) } returns mockk() },
                startSatsendring = mockk<StartSatsendring>().apply {
                    every {
                        sjekkOgOpprettSatsendringVedGammelSats(
                            any<String>(),
                        )
                    } returns true
                },
            ).doTask(
                BehandleFødselshendelseTask.opprettTask(
                    BehandleFødselshendelseTaskDTO(
                        nyBehandling = NyBehandlingHendelse(
                            morsIdent = randomFnr(),
                            barnasIdenter = listOf("31018721832"),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `skal opprette oppggavetask dersom det oppstår en funksjonell feil ved fødselshendelse uten eksisterende behandling`() {
        val taskRepositoryWrapper = mockk<TaskRepositoryWrapper>().also { every { it.save(any()) } returns mockk() }
        val randomAktør = randomAktør()
        mockkObject(OpprettVurderKonsekvensForYtelseOppgave)

        BehandleFødselshendelseTask(
            behandlingHentOgPersisterService = mockk(),
            fagsakService = mockk<FagsakService>().apply { every { hentNormalFagsak(any()) } returns null },
            oppgaveService = mockk(),
            taskRepositoryWrapper = taskRepositoryWrapper,
            personidentService = mockk<PersonidentService>().apply { every { hentAktør(any()) } returns randomAktør },
            autovedtakStegService = mockk(),
            velgFagsystemService = mockk<VelgFagSystemService>().apply {
                every<Pair<FagsystemRegelVurdering, FagsystemUtfall>> { velgFagsystem(any()) } returns Pair(
                    FagsystemRegelVurdering.SEND_TIL_BA,
                    FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                )
            },
            infotrygdFeedService = mockk(),
            startSatsendring = mockk<StartSatsendring>().apply {
                every {
                    sjekkOgOpprettSatsendringVedGammelSats(
                        any<String>(),
                    )
                }.throws(FunksjonellFeil("funksjonell feil"))
            },
        ).doTask(
            BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(
                    nyBehandling = NyBehandlingHendelse(
                        morsIdent = randomFnr(),
                        barnasIdenter = listOf("31018721832"),
                    ),
                ),
            ),
        )
        verify(exactly = 1) {
            OpprettVurderKonsekvensForYtelseOppgave.opprettTask(
                    ident = randomAktør.aktørId,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    beskrivelse = "Saksbehandler må vurdere konsekvens for ytelse fordi fødselshendelsen ikke kunne håndteres automatisk",
                )
        }
    }

    @Test
    fun `skal opprette oppgavetask dersom det oppstår en funksjonell feil ved fødselshendelse med eksisterende behandling`() {
        val taskRepositoryWrapper = mockk<TaskRepositoryWrapper>().also { every { it.save(any()) } returns mockk() }
        val oppgaveService = mockk<OppgaveService>().apply { every { opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns "Begrunnelse for oppgave" }
        val randomAktør = randomAktør()
        val behandling = lagBehandling()
        mockkObject(OpprettVurderKonsekvensForYtelseOppgave)

        BehandleFødselshendelseTask(
            fagsakService = mockk<FagsakService>().apply { every { hentNormalFagsak(any()) } returns behandling.fagsak },
            behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>().apply { every { finnAktivForFagsak(any()) } returns behandling },
            oppgaveService = oppgaveService,
            taskRepositoryWrapper = taskRepositoryWrapper,
            personidentService = mockk<PersonidentService>().apply { every { hentAktør(any()) } returns randomAktør },
            autovedtakStegService = mockk(),
            velgFagsystemService = mockk<VelgFagSystemService>().apply {
                every<Pair<FagsystemRegelVurdering, FagsystemUtfall>> { velgFagsystem(any()) } returns Pair(
                    FagsystemRegelVurdering.SEND_TIL_BA,
                    FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                )
            },
            infotrygdFeedService = mockk(),
            startSatsendring = mockk<StartSatsendring>().apply {
                every {
                    sjekkOgOpprettSatsendringVedGammelSats(
                        any<String>(),
                    )
                }.throws(FunksjonellFeil("funksjonell feil"))
            },
        ).doTask(
            BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(
                    nyBehandling = NyBehandlingHendelse(
                        morsIdent = randomFnr(),
                        barnasIdenter = listOf("31018721832"),
                    ),
                ),
            ),
        )
        verify(exactly = 1) {
            oppgaveService.opprettOppgaveForManuellBehandling(
                behandling = behandling,
                begrunnelse = ManuellOppgaveType.FØDSELSHENDELSE.toString(),
                opprettLogginnslag = false,
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
            )
        }
    }

    private fun settOppBehandleFødselshendelseTask(
        autovedtakStegService: AutovedtakStegService,
    ): BehandleFødselshendelseTask =
        BehandleFødselshendelseTask(
            behandlingHentOgPersisterService = mockk(),
            fagsakService = mockk(),
            oppgaveService = mockk(),
            taskRepositoryWrapper = mockk(),
            autovedtakStegService = autovedtakStegService,
            velgFagsystemService = mockk<VelgFagSystemService>().apply {
                every<Pair<FagsystemRegelVurdering, FagsystemUtfall>> { velgFagsystem(any()) } returns Pair(
                    FagsystemRegelVurdering.SEND_TIL_BA,
                    FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                )
            },
            infotrygdFeedService = mockk(),
            personidentService = mockk<PersonidentService>().apply { every { hentAktør(any()) } returns mockk() },
            startSatsendring = mockk<StartSatsendring>().apply {
                every {
                    sjekkOgOpprettSatsendringVedGammelSats(
                        any<String>(),
                    )
                } returns false
            },
        )
}
