package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.steg.SendTilManuellBehandling
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.nare.core.evaluations.Resultat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
                     beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
                     maxAntallFeil = 3)
class BehandleFødselshendelseTask(
        private val stegService: StegService,
        private val sendTilManuellBehandling: SendTilManuellBehandling,
        private val featureToggleService: FeatureToggleService
) : AsyncTaskStep {
    val stansetIAutomatiskFiltreringMetrics = Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "filtrering")
    val stansetIAutomatiskVilkårsvurderingMetrics = Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "vilkaarsvurdering")
    val passertFiltreringEllerVilkårsvurderingMetrics =  Metrics.counter("familie.ba.sak.henvendelse.passert")

    @Transactional(noRollbackFor = [Feil::class])
    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO = objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)
        try {
            LOG.info("Kjører BehandleFødselshendelseTask")
            val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling
            val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)

            var stanseAutomatiskBehandling = stegService.evaluerFiltreringsReglerForFødselshendelse(behandling, nyBehandling.barnasIdenter[0]).resultat != Resultat.JA

            if (stanseAutomatiskBehandling) {
                stansetIAutomatiskFiltreringMetrics.increment()

                stanseAutomatiskBehandling = stegService.evaluerVilkårForFødselshendelse(behandling, nyBehandling.søkersIdent) != BehandlingResultatType.INNVILGET

                if (stanseAutomatiskBehandling) {
                    stansetIAutomatiskVilkårsvurderingMetrics.increment()
                }
            }

            if(stanseAutomatiskBehandling) {
                if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")
                    && !featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")) {
                        sendTilManuellBehandling.opprettOppgave(behandlingId = behandling.id)
                } else {
                        LOG.info("Lag opprettOppgaveTask er skrudd av i miljø eller behandlingen av fødselshendelsen var innvilget")
                }
            } else {
                passertFiltreringEllerVilkårsvurderingMetrics.increment()
                // TODO: Trigge iverkesette henvendelse
            }

            if (featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")) {
                throw KontrollertRollbackException()
            }
        } catch (e: KontrollertRollbackException) {
            LOG.info("Rollback utført. Data ikke persistert.")
        } catch (e: Feil) {
            LOG.info("FødselshendelseTask kjørte med Feil=${e.frontendFeilmelding}")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "behandleFødselshendelseTask"
        val LOG = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(behandleFødselshendelseTaskDTO: BehandleFødselshendelseTaskDTO): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(behandleFødselshendelseTaskDTO),
                    properties = Properties().apply {
                        this["morsIdent"] = behandleFødselshendelseTaskDTO.nyBehandling.morsIdent
                    }
            )
        }
    }
}

class KontrollertRollbackException : RuntimeException()