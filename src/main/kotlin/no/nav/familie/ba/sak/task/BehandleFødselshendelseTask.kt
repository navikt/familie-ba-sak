package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.apache.commons.lang3.StringUtils.substring
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


@Service
@TaskStepBeskrivelse(
        taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
        beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
        maxAntallFeil = 3
)
class BehandleFødselshendelseTask(
        private val fødselshendelseService: FødselshendelseService,
        private val velgFagsystemService: VelgFagSystemService,
        private val infotrygdFeedService: InfotrygdFeedService
) : AsyncTaskStep {

    private val dagerSidenBarnBleFødt: DistributionSummary = Metrics.summary("behandling.tid")

    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO =
                objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)
        logger.info("Behandler fødselshendelse")
        secureLogger.info("Behandler fødselshendelse ${behandleFødselshendelseTaskDTO.nyBehandling}")

        val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling

        nyBehandling.barnasIdenter.forEach {
            // En litt forenklet løsning for å hente fødselsdato uten å kalle PDL. Gir ikke helt riktige data, men godt nok.
            val dagerSidenBarnetBleFødt = ChronoUnit.DAYS.between(LocalDate.parse(it.substring(0, 6), DateTimeFormatter.ofPattern("ddMMyy")), LocalDateTime.now())
            dagerSidenBarnBleFødt.record(dagerSidenBarnetBleFødt.toDouble())
        }

        when (velgFagsystemService.velgFagsystem(nyBehandling)) {
            FagsystemRegelVurdering.SEND_TIL_BA -> fødselshendelseService.behandleFødselshendelse(nyBehandling = nyBehandling)
            FagsystemRegelVurdering.SEND_TIL_INFOTRYGD -> {
                infotrygdFeedService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)
            }
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