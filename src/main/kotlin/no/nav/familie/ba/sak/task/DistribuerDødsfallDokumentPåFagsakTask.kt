package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.sentry.Sentry
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.mottakerErDødUtenDødsboadresse
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

const val ANTALL_SEKUNDER_I_EN_UKE = 604800L

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerDødsfallDokumentPåFagsakTask.TASK_STEP_TYPE,
    beskrivelse = "Send dødsfall dokument til Dokdist",
    triggerTidVedFeilISekunder = ANTALL_SEKUNDER_I_EN_UKE,
    // ~8 måneder dersom vi prøver én gang i uka.
    // Tasken skal stoppe etter 6 måneder, så om vi kommer hit har det skjedd noe galt.
    maxAntallFeil = 4 * 8,
    settTilManuellOppfølgning = true,
)
class DistribuerDødsfallDokumentPåFagsakTask(
    private val dokumentService: DokumentService
) : AsyncTaskStep {

    private val antallBrevIkkeDistribuertUkjentDødsboadresse: Map<Brevmal, Counter> =
        mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
            Metrics.counter(
                "brev.ikke.sendt.ukjent.dodsbo",
                "brevtype", it.visningsTekst
            )
        }

    override fun doTask(task: Task) {
        val distribuerDødsfallDokumentPåFagsakTask =
            objectMapper.readValue(task.payload, DistribuerDødsfallDokumentPåFagsakDTO::class.java)

        val journalpostId = distribuerDødsfallDokumentPåFagsakTask.journalpostId
        val brevmal = distribuerDødsfallDokumentPåFagsakTask.brevmal

        val erTaskEldreEnn6Mnd = task.opprettetTid.isBefore(LocalDateTime.now().minusMonths(6))

        if (erTaskEldreEnn6Mnd) {
            logger.info("Stopper \"DistribuerDødsfallDokumentPåFagsakTask\" fordi den er eldre enn 6 måneder.")
            antallBrevIkkeDistribuertUkjentDødsboadresse[brevmal]?.increment()
        } else {
            try {
                dokumentService.prøvDistribuerBrevOgLoggHendelse(
                    journalpostId = journalpostId,
                    behandlingId = null,
                    loggBehandlerRolle = BehandlerRolle.SYSTEM,
                    brevmal = brevmal
                )
            } catch (e: Exception) {
                if (e is RessursException && mottakerErDødUtenDødsboadresse(e)) {
                    logger.info("Klarte ikke å distribuere \"${brevmal.hentPresentabelVisningstekst()}\" på journalpost $journalpostId. Prøver igjen om 7 dager.")
                    throw e
                } else {
                    Sentry.captureException(e)
                    throw e
                }
            }
        }
    }

    companion object {
        fun opprettTask(journalpostId: String, brevmal: Brevmal): Task {
            return Task(
                type = this.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    DistribuerDødsfallDokumentPåFagsakDTO(
                        journalpostId,
                        brevmal
                    )
                )
            )
        }

        const val TASK_STEP_TYPE = "distribuerDokumentPåFagsak"
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class DistribuerDødsfallDokumentPåFagsakDTO(
    val journalpostId: String,
    val brevmal: Brevmal,
)
