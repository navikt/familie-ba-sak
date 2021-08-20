package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
        private val dokumentService: DokumentService,
        private val taskRepository: TaskRepository,
) : BehandlingSteg<DistribuerDokumentDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: DistribuerDokumentDTO): StegType {
        logger.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")

        dokumentService.distribuerBrevOgLoggHendelse(journalpostId = data.journalpostId,
                                                     behandlingId = data.behandlingId,
                                                     loggBehandlerRolle = BehandlerRolle.SYSTEM,
                                                     brevType = data.brevType)

        val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
                personIdent = data.personIdent,
                behandlingsId = data.behandlingId)
        taskRepository.save(ferdigstillBehandlingTask)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.DISTRIBUER_VEDTAKSBREV
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DistribuerVedtaksbrev::class.java)
    }
}