package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fødselshendelse.FødselshendelseService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
                     beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
                     maxAntallFeil = 3)
class BehandleFødselshendelseTask(
        private val fødselshendelseService: FødselshendelseService,
        private val fødselshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository) :
        AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO = objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)
        LOG.info("Kjører BehandleFødselshendelseTask")

        val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling
        val fødselshendelseSkalBehandlesHosInfotrygd = fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(nyBehandling.morsIdent, nyBehandling.barnasIdenter)

        // Vi har overtatt ruting.
        // Pr. nå sender vi alle hendelser til infotrygd.
        // behandleHendelseIBaSak skal gjøre en "dry run", kun for metrikkers skyld, og skal hverken lage oppgave eller vedtak.
        // Koden under fjernes når vi går live.
        fødselshendelseService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)
        antallAutomatiskeBehandlingerOpprettet.increment()
        behandleHendelseIBaSak(nyBehandling)

        // Dette er flyten, slik den skal se ut når vi går "live".
        //
        // if (fødselshendelseSkalBehandlesHosInfotrygd) {
        //     fødselshendelseService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)
        // } else {
        //     behandleHendelseIBaSak(nyBehandling)
        // }
        //
        // Når vi går live skal ba-sak behandle saker som ikke er løpende i infotrygd.
        // Etterhvert som vi kan behandle flere typer saker, utvider vi fødselshendelseSkalBehandlesHosInfotrygd.
    }

    private fun behandleHendelseIBaSak(nyBehandling: NyBehandlingHendelse) {
        try {
            fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(nyBehandling)
        } catch (e: KontrollertRollbackException) {
            when (e.fødselshendelsePreLansering) {
                null -> LOG.error("Rollback har blitt trigget, men data fra fødselshendelse mangler")
                else -> fødselshendelsePreLanseringRepository.save(e.fødselshendelsePreLansering.copy(id = 0))
            }
            LOG.info("Rollback utført. Data ikke persistert.")
        } catch (e: Throwable) {
            LOG.info("FødselshendelseTask kjørte med Feil=${e.message}")

            if (e is Feil) {
                secureLogger.info("FødselshendelseTask kjørte med Feil=${e.frontendFeilmelding}", e)
            } else {
                secureLogger.info("FødselshendelseTask feilet!", e)
            }
            throw e
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "behandleFødselshendelseTask"
        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
        val antallAutomatiskeBehandlingerOpprettet = Metrics.counter(
                "behandling.opprettet.automatisk",
                "type",
                BehandlingType.FØRSTEGANGSBEHANDLING.name,
                "beskrivelse",
                BehandlingType.FØRSTEGANGSBEHANDLING.visningsnavn)

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

data class KontrollertRollbackException(val fødselshendelsePreLansering: FødselshendelsePreLansering?) : RuntimeException()