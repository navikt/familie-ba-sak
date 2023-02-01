package no.nav.familie.ba.sak.kjerne.brev

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.task.DistribuerDødsfallDokumentPåFagsakTask
import no.nav.familie.http.client.RessursException
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DokumentDistribueringService(
    private val taskService: TaskService,
    private val integrasjonClient: IntegrasjonClient,
    private val loggService: LoggService
) {

    fun prøvDistribuerBrevOgLoggHendelse(
        journalpostId: String,
        behandlingId: Long?,
        loggBehandlerRolle: BehandlerRolle,
        brevmal: Brevmal
    ) = try {
        distribuerBrevOgLoggHendlese(journalpostId, behandlingId, brevmal, loggBehandlerRolle)
    } catch (ressursException: RessursException) {
        logger.info("Klarte ikke å distribuere brev til journalpost $journalpostId på behandling $behandlingId. Httpstatus ${ressursException.httpStatus}")
        secureLogger.info(
            "Klarte ikke å distribuere brev til journalpost $journalpostId på behandling $behandlingId.\n" +
                "Httpstatus: ${ressursException.httpStatus}\n" +
                "Melding: ${ressursException.cause?.message}"
        )

        when {
            mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException) && behandlingId != null ->
                loggBrevIkkeDistribuertUkjentAdresse(journalpostId, behandlingId, brevmal)

            mottakerErDødUtenDødsboadresse(ressursException) && behandlingId != null ->
                håndterMottakerDødIngenAdressePåBehandling(journalpostId, brevmal, behandlingId)

            dokumentetErAlleredeDistribuert(ressursException) ->
                logger.warn(alleredeDistribuertMelding(journalpostId, behandlingId))

            else -> throw ressursException
        }
    }

    internal fun håndterMottakerDødIngenAdressePåBehandling(
        journalpostId: String,
        brevmal: Brevmal,
        behandlingId: Long
    ) {
        val task = DistribuerDødsfallDokumentPåFagsakTask.opprettTask(journalpostId = journalpostId, brevmal = brevmal)
        taskService.save(task)
        logger.info("Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent dødsboadresse.")
        loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevmal.visningsTekst
        )
    }

    internal fun loggBrevIkkeDistribuertUkjentAdresse(
        journalpostId: String,
        behandlingId: Long,
        brevMal: Brevmal
    ) {
        logger.info("Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent adresse.")
        loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevMal.visningsTekst
        )
        antallBrevIkkeDistribuertUkjentAndresse[brevMal]?.increment()
    }

    private fun distribuerBrevOgLoggHendlese(
        journalpostId: String,
        behandlingId: Long?,
        brevMal: Brevmal,
        loggBehandlerRolle: BehandlerRolle
    ) {
        integrasjonClient.distribuerBrev(journalpostId = journalpostId, distribusjonstype = brevMal.distribusjonstype)

        if (behandlingId != null) {
            loggService.opprettDistribuertBrevLogg(
                behandlingId = behandlingId,
                tekst = brevMal.visningsTekst,
                rolle = loggBehandlerRolle
            )
        }

        antallBrevSendt[brevMal]?.increment()
    }

    private val antallBrevSendt: Map<Brevmal, Counter> = mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
        Metrics.counter(
            "brev.sendt",
            "brevtype",
            it.visningsTekst
        )
    }

    private val antallBrevIkkeDistribuertUkjentAndresse: Map<Brevmal, Counter> =
        mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
            Metrics.counter(
                "brev.ikke.sendt.ukjent.andresse",
                "brevtype",
                it.visningsTekst
            )
        }

    fun alleredeDistribuertMelding(journalpostId: String, behandlingId: Long?) =
        "Journalpost med Id=$journalpostId er allerede distiribuert. Hopper over distribuering." +
            if (behandlingId != null) " BehandlingId=$behandlingId." else ""

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
