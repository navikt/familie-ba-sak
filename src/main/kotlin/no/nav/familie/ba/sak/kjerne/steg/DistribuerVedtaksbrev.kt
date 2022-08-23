package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
    private val dokumentService: DokumentService,
    private val taskRepository: TaskRepositoryWrapper,
) : BehandlingSteg<DistribuerDokumentDTO> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: DistribuerDokumentDTO
    ): StegType {
        logger.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")

        dokumentService.prøvDistribuerBrevOgLoggHendelse(
            journalpostId = data.journalpostId,
            behandlingId = data.behandlingId,
            loggBehandlerRolle = BehandlerRolle.SYSTEM,
            brevmal = data.brevmal
        )

        val søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer()

        val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
            søkerIdent = søkerIdent,
            behandlingsId = data.behandlingId!!
        )
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
