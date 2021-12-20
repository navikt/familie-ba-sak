package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
    private val dokumentService: DokumentService,
    private val taskRepository: TaskRepositoryWrapper,
    private val personidentService: PersonidentService,
) : BehandlingSteg<DistribuerDokumentDTO> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: DistribuerDokumentDTO
    ): StegType {
        logger.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")

        dokumentService.distribuerBrevOgLoggHendelse(
            journalpostId = data.journalpostId,
            behandlingId = data.behandlingId,
            loggBehandlerRolle = BehandlerRolle.SYSTEM,
            brevMal = data.brevmal
        )
        val aktør = personidentService.hentOgLagreAktør(data.personIdent)

        val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
            søkerPersonIdent = aktør.aktivFødselsnummer(),
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
