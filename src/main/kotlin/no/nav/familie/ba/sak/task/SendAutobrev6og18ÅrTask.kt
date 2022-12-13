package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.Autobrev6og18ÅrService
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
    beskrivelse = "Send autobrev for barn som fyller 6 og 18 år til Dokdist",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = (60 * 60 * 24).toLong(),
    settTilManuellOppfølgning = true
)
class SendAutobrev6og18ÅrTask(
    private val autobrev6og18ÅrService: Autobrev6og18ÅrService,
    private val taskRepository: TaskRepositoryWrapper
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, Autobrev6og18ÅrDTO::class.java)

        if (!LocalDate.now().toYearMonth().equals(autobrevDTO.årMåned)) {
            throw Feil("Task for autobrev må kjøres innenfor måneden det skal sjekkes mot.")
        }
        try {
            autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrevDTO)
        } catch (e: RuntimeException) {
            if (e.message == KJENT_FEIL_RELATERT_TIL_BARNAS_ATY && !autobrevDTO.brukAlternativAtyMetodeForBarna) {
                forsøkPåNyttMedAlternativMetodeForBarnasAty(task, autobrevDTO)
            } else {
                throw e
            }
        }
    }

    private fun forsøkPåNyttMedAlternativMetodeForBarnasAty(
        task: Task,
        autobrevDTO: Autobrev6og18ÅrDTO
    ) {
        taskRepository.save(
            task.copy(
                payload = objectMapper.writeValueAsString(
                    Autobrev6og18ÅrDTO(
                        fagsakId = autobrevDTO.fagsakId,
                        alder = autobrevDTO.alder,
                        årMåned = autobrevDTO.årMåned,
                        brukAlternativAtyMetodeForBarna = true
                    )
                )
            )
        )
        throw RekjørSenereException(
            årsak = "Prøver igjen med bryteren ny-metode-genererer-aty-barna overstyrt som en workaround for kjent feil",
            triggerTid = LocalDateTime.now().plusMinutes(1)
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "sendAutobrevVed6og18År"
        const val KJENT_FEIL_RELATERT_TIL_BARNAS_ATY =
            "Steg 'Journalfør vedtaksbrev' kan ikke settes på behandling i kombinasjon med status UTREDES"
    }
}
