package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.ekstern.pensjon.HentAlleIdenterTilPsysResponseDTO
import no.nav.familie.ba.sak.ekstern.pensjon.Meldingstype
import no.nav.familie.ba.sak.ekstern.pensjon.Meldingstype.DATA
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.HentAlleIdenterTilPsysRequestDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Henter alle identer som har barnetrygd for gjeldende år til psys",
    maxAntallFeil = 1,
)
class HentAlleIdenterTilPsysTask(
    private val kafkaProducer: KafkaProducer,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val envService: EnvService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(HentAlleIdenterTilPsysTask::class.java)

    override fun doTask(task: Task) {
        objectMapper.readValue(task.payload, HentAlleIdenterTilPsysRequestDTO::class.java).run {
            logger.info("Starter med å hente alle identer fra DB for request $requestId")
            val identerFraDB = andelTilkjentYtelseRepository.finnIdenterMedLøpendeBarnetrygdForGittÅr(år)
            logger.info("Ferdig med å hente alle identer fra DB for request $requestId")

            logger.info("Starter med å hente alle identer fra Infotrygd for request $requestId")
            val identerFraInfotrygd = when {
                envService.erPreprod() -> emptyList() // Tar ikke med personer fra infotrygd i preprod siden de ikke finnes i PDL
                else -> infotrygdBarnetrygdClient.hentPersonerMedBarnetrygdTilPensjon(år)
            }
            logger.info("Ferdig med å hente alle identer fra Infotrygd for request $requestId")

            logger.info("Starter på å sende alle identer til kafka for request $requestId")
            val identer = identerFraDB.plus(identerFraInfotrygd).distinct()
            val dataMal =
                HentAlleIdenterTilPsysResponseDTO(DATA, requestId, personident = null, antallIdenterTotalt = identer.size)

            kafkaProducer.sendIdentTilPSys(dataMal.copy(meldingstype = Meldingstype.START))
            identer.forEach {
                kafkaProducer.sendIdentTilPSys(dataMal.copy(personident = it))
            }
            kafkaProducer.sendIdentTilPSys(dataMal.copy(meldingstype = Meldingstype.SLUTT))
            logger.info("Ferdig med å sende alle identer til kafka for request $requestId")
        }
    }

    override fun onCompletion(task: Task) {
    }

    companion object {
        fun lagTask(år: Int, uuid: UUID): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(HentAlleIdenterTilPsysRequestDTO(år = år, requestId = uuid)),
                properties = Properties().apply {
                    this["år"] = år.toString()
                    this["requestId"] = uuid.toString()
                    this["callId"] = uuid.toString()
                },
            )
        }
        const val TASK_STEP_TYPE = "hentAlleIdenterTilPsys"
    }
}
