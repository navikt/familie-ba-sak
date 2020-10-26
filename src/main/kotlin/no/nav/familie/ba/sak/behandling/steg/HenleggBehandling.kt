package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class HenleggBehandling(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
) : BehandlingSteg<HenleggBehandlingInfo> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: HenleggBehandlingInfo): StegType {
        behandlingService.settBehandlingResultatTilHenlagt(behandling.id)

        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.FERDIGSTILLE_BEHANDLING)
        //TODO: Trenger man hente personIdent når den ikke blir brukt?
        opprettFerdigstillBehandling(behandling.id, behandling.fagsak.hentAktivIdent().ident)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.HENLEGG_SØKNAD
    }

    private fun opprettFerdigstillBehandling(behandlingsId: Long, personIdent: String) {
        val ferdigstillBehandling =
                FerdigstillBehandlingTask.opprettTask(behandlingsId = behandlingsId, personIdent = personIdent)
        taskRepository.save(ferdigstillBehandling)
    }
}