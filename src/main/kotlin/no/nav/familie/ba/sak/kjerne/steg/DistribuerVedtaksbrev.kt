package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentDistribueringService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
    private val dokumentDistribueringService: DokumentDistribueringService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService
) : BehandlingSteg<DistribuerDokumentDTO> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: DistribuerDokumentDTO
    ): StegType {
        logger.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")
        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelse(
            distribuerDokumentDTO = data,
            loggBehandlerRolle = BehandlerRolle.SYSTEM
        )

        val søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer()

        val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
            søkerIdent = søkerIdent,
            behandlingsId = data.behandlingId!!
        )
        taskRepository.save(ferdigstillBehandlingTask)

        // Når vi sender vedtaksbrev til flere mottakere, DistribuerVedtaksbrev steg kjører flere ganger samtidig
        // Det kan medføre ObjectOptimisticLockingFailureException på DistribuerDokument task
        // for å fikse det sjekkes behandling sin nåværrende steg etter distribuering og slipper
        // og returnerer BEHANDLING_AVSLUTTET steg hvis behandling er allerede ferdigstilt slik at
        // da steg service ikke prøver å oppdatere på nytt
        val behandlingMedNåværrendeSteg = behandlingHentOgPersisterService.hent(behandling.id)
        if (behandlingMedNåværrendeSteg.steg == StegType.FERDIGSTILLE_BEHANDLING) {
            return StegType.BEHANDLING_AVSLUTTET
        }

        return hentNesteStegForNormalFlyt(behandlingHentOgPersisterService.hent(behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.DISTRIBUER_VEDTAKSBREV
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DistribuerVedtaksbrev::class.java)
    }
}
