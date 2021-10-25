package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.io.StringReader
import java.util.Properties

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
    val taskRepositoryForAnnullertFødsel: TaskRepositoryForAnnullertFødsel,
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        var dto = objectMapper.readValue(task.payload, BehandleAnnullerFødselDto::class.java)
        var barnasIdenter = dto.barnasIdenter.map { PersonIdent(it.toString()) }

        var tasker = taskRepositoryForAnnullertFødsel.hentTaskForTidligereHendelse(dto.tidligereHendelseId)
        if (tasker.isEmpty()) {
            logger.info("Finnes ikke åpen task for annullertfødsel tidligere Id = ${dto.tidligereHendelseId}. Forsøker å finne aktiv behandling.")
            if (personRepository.findByPersonIdenter(barnasIdenter).any {
                behandlingRepository.finnBehandling(it.personopplysningGrunnlag.behandlingId).aktiv
            }
            ) {
                logger.warn("Finnes aktiv behandling(er) for annullert fødselshendelse.")
            } else {
                logger.info("Finnes ikke åpen task eller aktiv behandling for annullertfødsel")
            }
        } else {
            logger.info("Finnes åpen task(er) for annullertfødsel tidligere Id = ${dto.tidligereHendelseId}")
            tasker.forEach {
                taskRepository.save(
                    taskRepository.findById(it).get()
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

        fun opprettTask(behandleAnnullerFødselDto: BehandleAnnullerFødselDto): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    behandleAnnullerFødselDto
                ),
            )
        }
    }
}

data class BehandleAnnullerFødselDto(val barnasIdenter: List<String>, val tidligereHendelseId: String)

@Repository
class TaskRepositoryForAnnullertFødsel(private val jdbcTemplate: JdbcTemplate) {

    fun hentTaskForTidligereHendelse(tidligereHendelseId: String): List<Long> {
        var query = "SELECT id, metadata FROM task t\n" +
            "WHERE t.status IN ('KLAR_TIL_PLUKK', 'UBEHANDLET', 'FEILET')\n" +
            "  AND t.type = 'behandleFødselshendelseTask'"
        var rowMapper = RowMapper { row, index ->
            Pair(
                row.getLong("id"),
                Properties().also { it.load(StringReader((row.getString("metadata")))) }
            )
        }
        return jdbcTemplate.query(query, rowMapper).filter { it.second.getProperty("callId") == tidligereHendelseId }
            .map {
                it.first
            }
    }
}
