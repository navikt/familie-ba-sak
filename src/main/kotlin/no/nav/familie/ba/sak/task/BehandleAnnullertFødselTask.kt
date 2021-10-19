package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleAnnullertFødselTask.TASK_STEP_TYPE,
    beskrivelse = "Behanle annullert fødsel",
    maxAntallFeil = 3
)
class BehandleAnnullertFødselTask(
    val taskRepository: TaskRepository,
    val behandlingRepository: BehandlingRepository,
    val personRepository: PersonRepository,
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val tidligereHendelseId = MDC.get(MDCConstants.MDC_CALL_ID)
        var barnasIdenter = objectMapper.readValue(task.payload, List::class.java)
            .map { PersonIdent(it.toString()) }
        var tasker =
            taskRepository.findByStatusIn(
                listOf(Status.KLAR_TIL_PLUKK, Status.UBEHANDLET, Status.FEILET),
                Pageable.unpaged()
            )
                .filter {
                    it.callId == tidligereHendelseId && (it.type == BehandleFødselshendelseTask.TASK_STEP_TYPE)
                }
        if (tasker.isEmpty()) {
            logger.info("Finnes ikke åpen task for annullertfødsel tidligere Id = ${tidligereHendelseId}. Forsøker å finne aktiv behandling.")
            if (personRepository.findByPersonIdenter(barnasIdenter).any {
                behandlingRepository.finnBehandling(it.personopplysningGrunnlag.behandlingId).aktiv
            }
            ) {
                logger.warn("Finnes aktiv behandling(er) for annullert fødselshendelse.")
            } else {
                logger.info("Finnes ikke åpen task eller aktiv behandling for annullertfødsel")
            }
        } else {
            logger.info("Finnes åpen task(er) for annullertfødsel tidligere Id = ${tidligereHendelseId}")
            tasker.forEach {
                taskRepository.save(
                    taskRepository.findById(it.id!!).get()
                        .avvikshåndter(avvikstype = Avvikstype.ANNET, årsak = AVVIKSÅRSAK, endretAv = "VL")
                )
            }
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "BehandleAnnullertFødselTask"
        const val AVVIKSÅRSAK = "Annuller fødselshendelse"
        private val logger = LoggerFactory.getLogger(BehandleAnnullertFødselTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTask(barnasIdenter: List<String>): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(barnasIdenter),
            )
        }
    }
}
