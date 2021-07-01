package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseServiceGammel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties


@Service
@TaskStepBeskrivelse(
        taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
        beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
        maxAntallFeil = 3
)
class BehandleFødselshendelseTask(
        private val fødselshendelseServiceGammel: FødselshendelseServiceGammel,
        private val velgFagSystemService: VelgFagSystemService,
        private val fagsakService: FagsakService,
        private val featureToggleService: FeatureToggleService,


        private val fødselshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository
) :
        AsyncTaskStep {


    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO =
                objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)
        logger.info("Kjører BehandleFødselshendelseTask")

        val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling

        fødselshendelseServiceGammel.fødselshendelseSkalBehandlesHosInfotrygd(
                nyBehandling.morsIdent,
                nyBehandling.barnasIdenter
        )

        // Vi har overtatt ruting.
        // Pr. nå sender vi alle hendelser til infotrygd.
        // Koden under fjernes når vi går live.
        // fødselshendelseService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)

        // Dette er flyten, slik den skal se ut når vi går "live".
        //


        if (featureToggleService.isEnabled(FeatureToggleConfig.AUTOMATISK_FØDSELSHENDELSE)) {

            when (velgFagSystemService.velgFagsystem(nyBehandling)) {
                VelgFagSystemService.FagsystemRegelVurdering.SEND_TIL_BA -> behandleHendelseIBaSak(nyBehandling)
                VelgFagSystemService.FagsystemRegelVurdering.SEND_TIL_INFOTRYGD -> fødselshendelseServiceGammel.sendTilInfotrygdFeed(
                        nyBehandling.barnasIdenter)
            }
        } else fødselshendelseServiceGammel.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)


        //     behandleHendelseIBaSak(nyBehandling)
        // }
        //
        // Når vi går live skal ba-sak behandle saker som ikke er løpende i infotrygd.
        // Etterhvert som vi kan behandle flere typer saker, utvider vi fødselshendelseSkalBehandlesHosInfotrygd.
    }

    private fun behandleHendelseIBaSak(nyBehandling: NyBehandlingHendelse) {
        try {
            fødselshendelseServiceGammel.opprettBehandlingOgKjørReglerForFødselshendelse(nyBehandling)
        } catch (e: KontrollertRollbackException) {
            when (e.fødselshendelsePreLansering) {
                null -> logger.error("Rollback har blitt trigget, men data fra fødselshendelse mangler")
                else -> fødselshendelsePreLanseringRepository.save(e.fødselshendelsePreLansering.copy(id = 0))
            }
            logger.info("Rollback utført. Data ikke persistert.")
        } catch (e: Throwable) {
            logger.error("FødselshendelseTask kjørte med Feil=${e.message}")

            if (e is Feil) {
                secureLogger.error("FødselshendelseTask kjørte med Feil=${e.frontendFeilmelding}", e)
            } else {
                secureLogger.error("FødselshendelseTask feilet!", e)
            }
            throw e
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "behandleFødselshendelseTask"
        private val logger = LoggerFactory.getLogger(BehandleFødselshendelseTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

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

data class KontrollertRollbackException(val fødselshendelsePreLansering: FødselshendelsePreLansering?) :
        RuntimeException()