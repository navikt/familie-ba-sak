package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringKafkaProducer
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.task.dto.AktiverMinsideDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = AktiverMinsideTask.TASK_STEP_TYPE,
    beskrivelse = "Aktiverer minside for ident",
    maxAntallFeil = 3,
)
class AktiverMinsideTask(
    private val minsideAktiveringKafkaProducer: MinsideAktiveringKafkaProducer,
    private val minsideAktiveringService: MinsideAktiveringService,
    private val aktørIdRepository: AktørIdRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val aktiverMinsideDTO =
            objectMapper.readValue(task.payload, AktiverMinsideDTO::class.java)

        val aktør =
            aktørIdRepository.findByAktørIdOrNull(aktiverMinsideDTO.aktørId)
                ?: throw Feil("Aktør med aktørId ${aktiverMinsideDTO.aktørId} finnes ikke")

        // TODO: Må sjekke om barn i fagsak til aktør har kode 6 eller 19 og aktøren selv ikke har kode 6 eller 19. Hvis dette er tilfellet skal vi ikke aktivere minside.
        // TODO: Det samme gjelder dersom aktør selv har kode 6 eller 19 og fagsaken er "skjermet barn"-fagsak

        if (minsideAktiveringService.harAktivertMinsideAktivering(aktør)) {
            logger.info("Minside er allerede aktivert for aktør: ${aktør.aktørId}")
            return
        }

        logger.info("Aktiverer minside for aktør: ${aktør.aktørId}")
        minsideAktiveringService.aktiverMinsideAktivering(aktør)
        minsideAktiveringKafkaProducer.aktiver(aktør.aktivFødselsnummer())
    }

    companion object {
        const val TASK_STEP_TYPE = "aktiverMinside"
        private val logger = LoggerFactory.getLogger(AktiverMinsideTask::class.java)

        fun opprettTask(aktørId: String): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(AktiverMinsideDTO(aktørId)),
            )
    }
}
