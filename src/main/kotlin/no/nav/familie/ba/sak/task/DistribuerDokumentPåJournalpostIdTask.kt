package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.sentry.Sentry
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.brev.DokumentDistribueringService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.mottakerErDødUtenDødsboadresse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.restklient.client.RessursException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

const val ANTALL_SEKUNDER_I_EN_UKE = 604800L

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerDokumentPåJournalpostIdTask.TASK_STEP_TYPE,
    beskrivelse = "Distribuer dokument på journalpostId",
    triggerTidVedFeilISekunder = ANTALL_SEKUNDER_I_EN_UKE,
    // ~8 måneder dersom vi prøver én gang i uka.
    // Tasken skal stoppe etter 6 måneder, så om vi kommer hit har det skjedd noe galt.
    maxAntallFeil = 4 * 8,
    settTilManuellOppfølgning = true,
)
class DistribuerDokumentPåJournalpostIdTask(
    private val dokumentDistribueringService: DokumentDistribueringService,
) : AsyncTaskStep {
    private val antallBrevIkkeDistribuertUkjentDødsboadresse: Map<Brevmal, Counter> =
        mutableListOf<Brevmal>().plus(Brevmal.entries).associateWith {
            Metrics.counter(
                "brev.ikke.sendt.ukjent.dodsbo",
                "brevtype",
                it.visningsTekst,
            )
        }

    @WithSpan
    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue(task.payload, DistribuerDokumentDTO::class.java)

        val brevmal = taskData.brevmal
        val erTaskEldreEnn6Mnd = task.opprettetTid.isBefore(LocalDateTime.now().minusMonths(6))

        if (erTaskEldreEnn6Mnd) {
            logger.info("Stopper \"DistribuerDødsfallDokumentPåFagsakTask\" fordi den er eldre enn 6 måneder.")
            antallBrevIkkeDistribuertUkjentDødsboadresse[brevmal]?.increment()
        } else {
            try {
                dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelse(
                    distribuerDokumentDTO = taskData,
                    loggBehandlerRolle = BehandlerRolle.SYSTEM,
                )
            } catch (e: Exception) {
                if (e is RessursException && mottakerErDødUtenDødsboadresse(e)) {
                    logger.info(
                        "Klarte ikke å distribuere \"${brevmal.visningsTekst}\" på journalpost " +
                            "${taskData.journalpostId}. Prøver igjen om 7 dager.",
                    )
                    throw e
                } else {
                    Sentry.captureException(e)
                    throw e
                }
            }
        }
    }

    companion object {
        fun opprettTask(distribuerDokumentDTO: DistribuerDokumentDTO): Task {
            check(distribuerDokumentDTO.behandlingId == null)

            val metadata =
                Properties().apply {
                    this["journalpostId"] = distribuerDokumentDTO.journalpostId
                    this["fagsakId"] = distribuerDokumentDTO.fagsakId.toString()
                    this[MDCConstants.MDC_CALL_ID] = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
                }

            return Task(
                type = this.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerDokumentDTO),
                triggerTid = LocalDateTime.now().plusMinutes(5),
                metadataWrapper = PropertiesWrapper(metadata),
            )
        }

        const val TASK_STEP_TYPE = "distribuerDokumentPåFagsak"
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
