package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.dokument.DokumentController
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class HenleggBehandling(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService,
        private val dokumentService: DokumentService,
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

        behandling.resultat = data.årsak.tilBehandlingsresultat()

        behandlingService.lagreEllerOppdater(behandling)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.HENLEGG_SØKNAD)
        opprettFerdigstillBehandling(behandling.id, behandling.fagsak.hentAktivIdent().ident)

        return hentNesteStegForNormalFlyt(behandling)
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