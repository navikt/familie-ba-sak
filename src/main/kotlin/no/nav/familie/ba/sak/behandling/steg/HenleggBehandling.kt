package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.dokument.DokumentController
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class HenleggBehandling(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService,
        private val dokumentService: DokumentService,
        private val vilkårsvurderingService: VilkårsvurderingService,
        private val oppgaveService: OppgaveService
) : BehandlingSteg<RestHenleggBehandlingInfo> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: RestHenleggBehandlingInfo): StegType {
        if(data.årsak == HenleggÅrsak.SØKNAD_TRUKKET) {
            sendBrev(behandling)
        }

        oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling).forEach {
            oppgaveService.ferdigstillOppgave(behandling.id, it.type)
        }

        loggService.opprettHenleggBehandling(behandling, data.årsak.beskrivelse, data.begrunnelse)

        val behandlingResultat = when (data.årsak) {
            HenleggÅrsak.FEILAKTIG_OPPRETTET -> BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET
            HenleggÅrsak.SØKNAD_TRUKKET -> BehandlingResultat.HENLAGT_SØKNAD_TRUKKET
        }

        behandling.resultat = behandlingResultat
        behandling.aktiv = false

        behandlingService.lagreEllerOppdater(behandling)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.HENLEGG_SØKNAD)
        opprettFerdigstillBehandling(behandling.id, behandling.fagsak.hentAktivIdent().ident)

        return StegType.FERDIGSTILLE_BEHANDLING
    }

    override fun stegType(): StegType {
        return StegType.HENLEGG_SØKNAD
    }

    private fun sendBrev(behandling: Behandling) {
        dokumentService.sendManueltBrev(behandling, DokumentController.ManueltBrevRequest(
                mottakerIdent = behandling.fagsak.hentAktivIdent().ident,
                brevmal = BrevType.HENLEGGE_TRUKKET_SØKNAD,
        ))
    }

    private fun opprettFerdigstillBehandling(behandlingsId: Long, personIdent: String) {
        val ferdigstillBehandling =
                FerdigstillBehandlingTask.opprettTask(behandlingsId = behandlingsId, personIdent = personIdent)
        taskRepository.save(ferdigstillBehandling)
    }
}