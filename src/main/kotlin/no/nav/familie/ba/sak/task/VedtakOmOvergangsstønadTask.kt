package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.AUTOMATISK_SATSENDRING_SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = VedtakOmOvergangsstønadTask.TASK_STEP_TYPE,
    beskrivelse = "Håndterer vedtak om overgangsstønad",
    maxAntallFeil = 3,
)
class VedtakOmOvergangsstønadTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val personidentService: PersonidentService,
    private val startSatsendring: StartSatsendring,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val personIdent = task.payload
        logger.info("Håndterer vedtak om overgangsstønad. Se secureLog for detaljer")
        secureLogger.info("Håndterer vedtak om overgangsstønad for person $personIdent.")

        val aktør = personidentService.hentAktør(personIdent)

        if (featureToggleService.isEnabled(AUTOMATISK_SATSENDRING_SMÅBARNSTILLEGG)) {
            val harOpprettetSatsendring = startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(personIdent)
            if (harOpprettetSatsendring) {
                throw RekjørSenereException(
                    årsak = "Satsendring må kjøre ferdig før man behandler autovedtak småbarnstillegg",
                    triggerTid = LocalDateTime.now().plusMinutes(60),
                )
            }
        }

        val responseFraService =
            autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                mottakersAktør = aktør,
                aktør = aktør,
                førstegangKjørt = task.opprettetTid,
            )
        secureLogger.info("Håndterte vedtak om overgangsstønad for person $personIdent:\n$responseFraService")
    }

    companion object {
        const val TASK_STEP_TYPE = "vedtakOmOvergangsstønadTask"
        private val logger = LoggerFactory.getLogger(VedtakOmOvergangsstønadTask::class.java)

        fun opprettTask(personIdent: String): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = personIdent,
                properties =
                    Properties().apply {
                        this["personIdent"] = personIdent
                    },
            )
    }
}
