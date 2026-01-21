package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.task.dto.MinsideDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DeaktiverMinsideTask.TASK_STEP_TYPE,
    beskrivelse = "Deaktiverer minside for ident",
    maxAntallFeil = 3,
)
class DeaktiverMinsideTask(
    private val minsideAktiveringService: MinsideAktiveringService,
    private val aktørIdRepository: AktørIdRepository,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val deaktiverMinsideDTO =
            jsonMapper.readValue(task.payload, MinsideDTO::class.java)

        val aktør =
            aktørIdRepository.findByAktørIdOrNull(deaktiverMinsideDTO.aktørId)
                ?: throw Feil("Aktør med aktørId ${deaktiverMinsideDTO.aktørId} finnes ikke")

        if (!minsideAktiveringService.harAktivertMinsideAktivering(aktør)) {
            logger.info("Minside er ikke aktivert for aktør: ${aktør.aktørId}")
            return
        }

        logger.info("Deaktiverer minside for aktør: ${aktør.aktørId}")
        minsideAktiveringService.deaktiverMinsideAktivering(aktør)
    }

    companion object {
        const val TASK_STEP_TYPE = "deaktiverMinside"
        private val logger = LoggerFactory.getLogger(DeaktiverMinsideTask::class.java)

        fun opprettTask(aktør: Aktør): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(MinsideDTO(aktør.aktørId)),
                properties =
                    Properties().apply {
                        this["aktørId"] = aktør.aktørId
                        this["fnr"] = aktør.aktivFødselsnummer()
                    },
            )
    }
}
