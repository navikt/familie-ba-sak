package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
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
    val personidentService: PersonidentService,
    val taskRepository: TaskRepository,
    val behandlingRepository: BehandlingRepository,
    val personRepository: PersonRepository,
    val taskRepositoryForAnnullertFødsel: TaskRepositoryForAnnullertFødsel,
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val dto = objectMapper.readValue(task.payload, BehandleAnnullerFødselDto::class.java)
        val barnasAktørIder = personidentService.hentAktørIder(dto.barnasIdenter)

        val tasker = taskRepositoryForAnnullertFødsel.hentTaskForTidligereHendelse(dto.tidligereHendelseId)
        if (tasker.isEmpty()) {
            logger.info("Finnes ikke åpen task for annullertfødsel tidligere Id = ${dto.tidligereHendelseId}. Forsøker å finne aktiv behandling.")
            if (personRepository.findByAktører(barnasAktørIder).any {
                behandlingRepository.finnBehandling(it.personopplysningGrunnlag.behandlingId).aktiv
            }
            ) {
                logger.error("Finnes aktiv behandling(er) for annullert fødselshendelse.")
                secureLogger.error("Finnes aktiv behandling(er) for annullert fødselshendelse. ${dto.barnasIdenter}")
                // TODO opprett vurder livshendelse oppgave for annulert melding
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
        val query = "SELECT id, metadata FROM task t\n" +
            "WHERE t.status IN ('KLAR_TIL_PLUKK', 'UBEHANDLET', 'FEILET')\n" +
            "  AND t.type = 'behandleFødselshendelseTask'"
        val rowMapper = RowMapper { row, _ ->
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
