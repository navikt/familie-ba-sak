package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class HenleggBehandling(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService
) : BehandlingSteg<RestHenleggBehandlingInfo> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: RestHenleggBehandlingInfo): StegType {
        loggService.opprettHenleggBehandling(behandling, data.årsak.name, data.begrunnelse)

        behandlingService.settBehandlingResultatTilHenlagt(behandling.id, data.årsak, data.begrunnelse)

        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.HENLEGG_SØKNAD)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.HENLAGT)
        opprettFerdigstillBehandling(behandling.id, behandling.fagsak.hentAktivIdent().ident)

        return StegType.FERDIGSTILLE_BEHANDLING
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