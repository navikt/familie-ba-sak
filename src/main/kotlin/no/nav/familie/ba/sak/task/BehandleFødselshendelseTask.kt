package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
    beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
    maxAntallFeil = 3,
)
class BehandleFødselshendelseTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val velgFagsystemService: VelgFagSystemService,
    private val infotrygdFeedService: InfotrygdFeedService,
    private val personidentService: PersonidentService,
    private val startSatsendring: StartSatsendring,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
    private val taskRepositoryWrapper: TaskRepositoryWrapper,
) : AsyncTaskStep {

    private val dagerSidenBarnBleFødt: DistributionSummary = Metrics.summary("foedselshendelse.dagersidenbarnfoedt")

    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO =
            objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)

        val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling

        logger.info("Behandler fødselshendelse")
        secureLogger.info("Behandler fødselshendelse, mor=${nyBehandling.morsIdent}, barna=${nyBehandling.barnasIdenter}")

        nyBehandling.barnasIdenter.forEach {
            // En litt forenklet løsning for å hente fødselsdato uten å kalle PDL. Gir ikke helt riktige data, men godt nok.
            val dagerSidenBarnetBleFødt =
                ChronoUnit.DAYS.between(
                    Fødselsnummer(it).fødselsdato,
                    LocalDateTime.now(),
                )
            dagerSidenBarnBleFødt.record(dagerSidenBarnetBleFødt.toDouble())
        }

        try {
            when (velgFagsystemService.velgFagsystem(nyBehandling).first) {
                FagsystemRegelVurdering.SEND_TIL_BA -> {
                    val harOpprettetSatsendring =
                        startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(nyBehandling.morsIdent)
                    if (harOpprettetSatsendring) {
                        throw RekjørSenereException(
                            "Satsendring skal kjøre ferdig før man behandler fødselsehendelse",
                            LocalDateTime.now().plusMinutes(60),
                        )
                    }
                    autovedtakStegService.kjørBehandlingFødselshendelse(
                        mottakersAktør = personidentService.hentAktør(
                            nyBehandling.morsIdent,
                        ),
                        behandlingsdata = nyBehandling,
                    )
                }

                FagsystemRegelVurdering.SEND_TIL_INFOTRYGD -> {
                    infotrygdFeedService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)
                }
            }
        } catch (e: FunksjonellFeil) {
            // else opprett task
            val aktør = personidentService.hentAktør(nyBehandling.morsIdent)
            val fagsak = fagsakService.hentNormalFagsak(aktør)
            val behandling = if (fagsak != null) { behandlingHentOgPersisterService.finnAktivForFagsak(fagsak.id) } else { null }

            if (fagsak != null && behandling != null) {
                // bruk den vanlige løypa for å opprette oppgave
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandling = behandling,
                    begrunnelse = ManuellOppgaveType.FØDSELSHENDELSE.toString(),
                    opprettLogginnslag = false,
                    manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE
                )
            } else {
                taskRepositoryWrapper.save(OpprettVurderKonsekvensForYtelseOppgave.opprettTask(
                    ident = aktør.aktørId,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    fristForFerdigstillelse = LocalDate.now().plusDays(5),
                    beskrivelse = "Saksbehandler må vurdere konsekvens for ytelse fordi fødselshendelsen ikke kunne håndteres automatisk"
                ))
            }
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "behandleFødselshendelseTask"
        private val logger = LoggerFactory.getLogger(BehandleFødselshendelseTask::class.java)

        fun opprettTask(behandleFødselshendelseTaskDTO: BehandleFødselshendelseTaskDTO): Task {
            val triggerTid = if (erKlokkenMellom21Og06()) kl06IdagEllerNesteDag() else LocalDateTime.now()
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(behandleFødselshendelseTaskDTO),
                properties = Properties().apply {
                    this["morsIdent"] = behandleFødselshendelseTaskDTO.nyBehandling.morsIdent
                },
            ).copy(
                triggerTid = triggerTid.plusDays(7),
            )
        }
    }
}
