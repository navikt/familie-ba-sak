package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class HenleggBehandling(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService,
        private val behandlingResultatService: BehandlingResultatService
) : BehandlingSteg<RestHenleggBehandlingInfo> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: RestHenleggBehandlingInfo): StegType {
        loggService.opprettHenleggBehandling(behandling, data.årsak.beskrivelse, data.begrunnelse)

        val behandlingResultatType = when (data.årsak) {
            HenleggÅrsak.FEILAKTIG_OPPRETTET -> BehandlingResultatType.HENLAGT_FEILAKTIG_OPPRETTET
            HenleggÅrsak.SØKNAD_TRUKKET -> BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET
        }
        behandlingResultatService.settBehandlingResultatTilHenlagt(behandling, behandlingResultatType)
        behandling.aktiv = false
        behandlingService.lagre(behandling)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.HENLEGG_SØKNAD)
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