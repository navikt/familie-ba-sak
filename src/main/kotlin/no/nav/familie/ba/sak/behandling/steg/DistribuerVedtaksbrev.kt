package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTask
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
        private val integrasjonClient: IntegrasjonClient,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService) : BehandlingSteg<DistribuerVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: DistribuerVedtaksbrevDTO,
                                      stegService: StegService?): StegType {
        DistribuerVedtaksbrevTask.LOG.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")
        integrasjonClient.distribuerVedtaksbrev(data.journalpostId)
        loggService.opprettDistribuertBrevLogg(behandlingId = data.behandlingId,
                                               tekst = "Vedtaksbrev er sendt til bruker")

        val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
                personIdent = data.personIdent,
                behandlingsId = data.behandlingId)
        taskRepository.save(ferdigstillBehandlingTask)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.DISTRIBUER_VEDTAKSBREV
    }
}