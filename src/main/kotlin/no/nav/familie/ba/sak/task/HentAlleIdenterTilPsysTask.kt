package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.task.dto.HentAlleIdenterTilPsysRequestDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.UUID

class HentAlleIdenterTilPsysTask(
    private val kafkaProducer: KafkaProducer,
    private val taskRepository: TaskRepositoryWrapper,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(HentAlleIdenterTilPsysTask::class.java)

    override fun doTask(task: Task) {
        val hentAlleIdenterDto = objectMapper.readValue(task.payload, HentAlleIdenterTilPsysRequestDTO::class.java)
        val identer = andelTilkjentYtelseRepository.finnIdenterMedLøpendeBarnetrygdForGittÅr(hentAlleIdenterDto.år)
        identer.forEach { kafkaProducer.sendIdentTilPSys(it, hentAlleIdenterDto.requestId) }
    }

    override fun onCompletion(task: Task) {
    }

    companion object {
        fun lagTask(år: String, uuid: UUID): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(HentAlleIdenterTilPsysRequestDTO(år = år, requestId = uuid)),
                properties = Properties().apply {
                    this["år"] = år
                    this["uuid"] = uuid
                },
            )
        }
        const val TASK_STEP_TYPE = "hentAlleIdenterTilPsys"
    }
}
