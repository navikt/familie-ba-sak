package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.brev.hentBrevtype
import no.nav.familie.ba.sak.brev.hentVedtaksbrevtype
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
        private val dokumentService: DokumentService,
        private val taskRepository: TaskRepository,
) : BehandlingSteg<DistribuerVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: DistribuerVedtaksbrevDTO): StegType {
        logger.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")

        val vedtakstype = hentBrevtype(behandling)

        dokumentService.distribuerBrevOgLoggHendelse(journalpostId = data.journalpostId,
                                                     behandlingId = data.behandlingId,
                                                     loggTekst = vedtakstype.visningsTekst,
                                                     loggBehandlerRolle = BehandlerRolle.SYSTEM,
                                                     brevType = vedtakstype)

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