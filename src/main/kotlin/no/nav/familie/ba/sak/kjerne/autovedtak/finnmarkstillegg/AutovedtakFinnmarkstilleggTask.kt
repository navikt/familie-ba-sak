package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
    beskrivelse = "Gjennomfør autovedtak for Finnmarkstillegg",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class AutovedtakFinnmarkstilleggTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val fagsakService: FagsakService,
    private val opprettTaskService: OpprettTaskService,
    private val startSatsendring: StartSatsendring,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()
        val aktør = fagsakService.hentAktør(fagsakId)
        val resultat =
            try {
                val harOpprettetSatsendring =
                    startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(fagsakId)
                if (harOpprettetSatsendring) {
                    throw RekjørSenereException(
                        "Satsendring skal kjøre ferdig før man behandler fødselsehendelse",
                        LocalDateTime.now().plusMinutes(60),
                    )
                }

                autovedtakStegService.kjørBehandlingFinnmarkstillegg(
                    mottakersAktør = aktør,
                    fagsakId = fagsakId,
                    førstegangKjørt = task.opprettetTid,
                )
            } catch (feil: AutovedtakSkalIkkeGjennomføresFeil) {
                "Ruller tilbake Finnmarkstillegg: ${feil.message}"
            } catch (feil: AutovedtakMåBehandlesManueltFeil) {
                opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(
                    fagsakId = fagsakId,
                    beskrivelse = feil.beskrivelse,
                )

                "Ruller tilbake Finnmarkstillegg: ${feil.message}"
            }

        logger.info(resultat)
        task.metadata["resultat"] = resultat.replace("\n", " ")
    }

    companion object {
        const val TASK_STEP_TYPE = "autovedtakFinnmarkstilleggTask"
        private val logger: Logger = LoggerFactory.getLogger(AutovedtakFinnmarkstilleggTask::class.java)
    }
}
