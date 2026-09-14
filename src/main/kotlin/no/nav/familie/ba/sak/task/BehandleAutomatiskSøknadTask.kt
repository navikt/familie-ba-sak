package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.Søknad
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.dto.BehandleAutomatiskSøknadTaskDTO
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleAutomatiskSøknadTask.TASK_STEP_TYPE,
    beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
    maxAntallFeil = 3,
)
class BehandleAutomatiskSøknadTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val oppgaveService: OppgaveService,
    private val stegService: StegService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val dto = jsonMapper.readValue(task.payload, BehandleAutomatiskSøknadTaskDTO::class.java)
        val nyBehandling = dto.nyBehandling
        val fagsakId = nyBehandling.fagsakId

        val søkerAktør = fagsakService.hentAktør(fagsakId)
        val søkersIdent = søkerAktør.aktivFødselsnummer()

        logger.info("Behandler automatisk søknad")
        secureLogger.info("Behandler automatisk søknad, søker=$søkersIdent, barna=${nyBehandling.barnasIdenter}")

        try {
            autovedtakStegService.kjørAutomatiskBehandlingSøknad(
                mottakersAktør = søkerAktør,
                søknad =
                    Søknad(
                        fagsakId = fagsakId,
                        søkersIdent = søkersIdent,
                        barnasIdenter = nyBehandling.barnasIdenter,
                    ),
            )
        } catch (feil: AutovedtakMåBehandlesManueltFeil) {
            val behandling = stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling)
            oppgaveService.opprettOppgaveForManuellBehandling(
                behandlingId = behandling.id,
                begrunnelse = "Ikke kandidat for automatisk behandling. Må behandles manuelt.",
                manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
                oppgavetype = Oppgavetype.BehandleSak,
            )
            logger.info("Henlegger automatisk behandling og oppretter oppgave: ${feil.message}")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "behandleAutomatiskSøknadTask"
        private val logger = LoggerFactory.getLogger(BehandleAutomatiskSøknadTask::class.java)

        fun opprettTask(
            dto: BehandleAutomatiskSøknadTaskDTO,
            nåtidspunkt: LocalDateTime = LocalDateTime.now(),
        ): Task {
            val properties =
                Properties().apply {
                    this["fagsakId"] = dto.nyBehandling.fagsakId.toString()
                }
            return Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(dto),
                properties = properties,
            ).copy(
                triggerTid = utledNesteTriggerTidIHverdagerForTask(nåtidspunkt), // TODO : Sjekk om vi skal vente til neste hverdag eller bare kjøre med en gang
            )
        }
    }
}
