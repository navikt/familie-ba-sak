package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringAktørValidator
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
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = AktiverMinsideTask.TASK_STEP_TYPE,
    beskrivelse = "Aktiverer minside for ident",
    maxAntallFeil = 3,
)
class AktiverMinsideTask(
    private val minsideAktiveringService: MinsideAktiveringService,
    private val aktørIdRepository: AktørIdRepository,
    private val minsideAktiveringAktørValidator: MinsideAktiveringAktørValidator,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val minsideDTO =
            jsonMapper.readValue(task.payload, MinsideDTO::class.java)

        val aktør =
            aktørIdRepository.findByAktørIdOrNull(minsideDTO.aktørId)
                ?: throw Feil("Aktør med aktørId ${minsideDTO.aktørId} finnes ikke")

        val kanAktivereMinsideForAktør = minsideAktiveringAktørValidator.kanAktivereMinsideForAktør(aktør)

        if (minsideAktiveringService.harAktivertMinsideAktivering(aktør)) {
            if (!kanAktivereMinsideForAktør) {
                logger.info("Minside er aktivert for aktør: ${aktør.aktørId}, men skulle ikke vært det. Deaktiverer derfor minside.")
                minsideAktiveringService.deaktiverMinsideAktivering(aktør)
                return
            }
            logger.info("Minside er allerede aktivert for aktør: ${aktør.aktørId}")
            return
        }

        if (!kanAktivereMinsideForAktør) {
            logger.info("Kan ikke aktivere minside for aktør: ${aktør.aktørId} - ingen fagsaker eller kun skjermet barn/institusjon")
            return
        }

        logger.info("Aktiverer minside for aktør: ${aktør.aktørId}")
        minsideAktiveringService.aktiverMinsideAktivering(aktør)
    }

    companion object {
        const val TASK_STEP_TYPE = "aktiverMinside"
        private val logger = LoggerFactory.getLogger(AktiverMinsideTask::class.java)

        fun opprettTask(
            aktør: Aktør,
            triggerTid: LocalDateTime = LocalDateTime.now(),
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(MinsideDTO(aktør.aktørId)),
                properties =
                    Properties().apply {
                        this["aktørId"] = aktør.aktørId
                        this["fnr"] = aktør.aktivFødselsnummer()
                    },
            ).medTriggerTid(triggerTid)
    }
}
