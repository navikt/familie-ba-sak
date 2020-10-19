package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.dokument.DokumentController
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrev(
        private val dokumentService: DokumentService,
        private val behandlingResultatService: BehandlingResultatService,
        private val taskRepository: TaskRepository) : BehandlingSteg<DistribuerVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: DistribuerVedtaksbrevDTO): StegType {
        LOG.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${data.journalpostId}")

        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)
        val loggTekst = when (behandlingResultat?.samletResultat) {
            BehandlingResultatType.INNVILGET -> "Vedtak om innvilgelse av barnetrygd"
            BehandlingResultatType.DELVIS_INNVILGET -> "Vedtak om innvilgelse av barnetrygd"
            BehandlingResultatType.OPPHØRT -> "Vedtak er opphørt"
            BehandlingResultatType.AVSLÅTT -> "Vedtak er avslått"
            else -> error("Samlet resultat (${behandlingResultat?.samletResultat}) er ikke gyldig for distribusjon av vedtaksbrev.")
        }
        dokumentService.distribuerBrevOgLoggHendelse(journalpostId = data.journalpostId,
                                                     behandlingId = data.behandlingId,
                                                     loggTekst = loggTekst,
                                                     loggBehandlerRolle = BehandlerRolle.SYSTEM,
                                                     brevType = DokumentController.BrevType.VEDTAK)

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
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}