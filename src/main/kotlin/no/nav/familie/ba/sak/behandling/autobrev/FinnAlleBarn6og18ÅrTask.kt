package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.autobrev.Autobrev6og18ÅrScheduler
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(taskStepType = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                     beskrivelse = "Send autobrev for barn som fyller 6 og 18 år til Dokdist",
                     maxAntallFeil = 3,
                     triggerTidVedFeilISekunder = 60 * 60 * 24)
class FinnAlleBarn6og18ÅrTask(
        private val behandlingService: BehandlingService,
        private val behandlingRepository: BehandlingRepository,
        private val personRepository: PersonRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        listOf<Long>(6, 4, 18).forEach { alder ->
            opprettBrevOppgave(
                    finnAllBarnMedFødselsdagInneværendeMåned(alder),
                    alder
            )
        }



        true -> {
            Autobrev6og18ÅrScheduler.LOG.info("*** Kjører Autobrev6og18ÅrScheduler!!!")
            listOf<Long>(6, 4, 18).forEach { alder ->
                val behandlingerMedBursdagsbarnInneværendeMåned =
                        behandlingRepository.finnBehandlingerMedPersonerMedFødselsdatoInnenfor(
                                LocalDate.now().førsteDagIInneværendeMåned().minusYears(alder),
                                LocalDate.now().sisteDagIMåned().minusYears(alder))

                Autobrev6og18ÅrScheduler.LOG.info("Funnet ${behandlingerMedBursdagsbarnInneværendeMåned.size} behandlinger med barn som fyller $alder år.")

                behandlingerMedBursdagsbarnInneværendeMåned.forEach {
                    val sendAutobrevTask = Task.nyTask(type = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                                                       payload = objectMapper.writeValueAsString(
                                                               Autobrev6og18ÅrDTO(
                                                                       behandlingsId = it.id,
                                                                       alder = alder.toInt())
                                                       ))
                    taskRepository.save(sendAutobrevTask)
                    Autobrev6og18ÅrScheduler.LOG.info("Opprettet SendAutobrev6og18ÅrTask")
                }
            }
        }
        false -> {
            Autobrev6og18ÅrScheduler.LOG.info("Ikke opprettet SendAutobrev6og18ÅrTask på denne poden")
        }
    }

    private fun opprettBrevOppgave(barnMedFødselsdagInneværendeMåned: Set<PersonIdent>, alder: int) {
        Task.nyTask(type = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(
                            Autobrev6og18ÅrDTO(
                                    personIdent = it,
                                    alder = alder))
    }

    private fun finnAllBarnMedFødselsdagInneværendeMåned(alder: Long): Set<PersonIdent> =
        LocalDate.now().minusYears(alder).let {
            personRepository.finnAllePersonerMedFødselsdatoInnenfor(it.førsteDagIInneværendeMåned(), it.sisteDagIMåned())
                    .map { it.personIdent }.toSet()
        }

companion object {

    const val TASK_STEP_TYPE = "FinnAlleBarn6og18ÅrTask"
    val LOG: Logger = LoggerFactory.getLogger(FinnAlleBarn6og18ÅrTask::class.java)
}
}

