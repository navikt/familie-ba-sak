package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE,
    beskrivelse = "Gjennomfør autovedtak for Svalbardtillegg",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class AutovedtakSvalbardtilleggTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()
        val aktør = fagsakService.hentAktør(fagsakId)
        val resultat =
            try {
                autovedtakStegService.kjørBehandlingSvalbardtillegg(
                    mottakersAktør = aktør,
                    fagsakId = fagsakId,
                    førstegangKjørt = task.opprettetTid,
                )
            } catch (feil: AutovedtakSkalIkkeGjennomføresFeil) {
                "Ruller tilbake Svalbardtillegg: ${feil.message}"
            } catch (feil: AutovedtakMåBehandlesManueltFeil) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = feil.behandlingId,
                    begrunnelse = feil.begrunnelse,
                    manuellOppgaveType = ManuellOppgaveType.SVALBARDTILLEGG,
                )

                "Ruller tilbake Svalbardtillegg: ${feil.message}"
            }

        logger.info(resultat)
    }

    companion object {
        const val TASK_STEP_TYPE = "autovedtakSvalbardtilleggTask"
        private val logger: Logger = LoggerFactory.getLogger(AutovedtakSvalbardtilleggTask::class.java)
    }
}
