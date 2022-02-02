package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevType
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.byggMottakerdata
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service

@Service
class HenleggBehandling(
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
    private val loggService: LoggService,
    private val dokumentService: DokumentService,
    private val oppgaveService: OppgaveService,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService
) : BehandlingSteg<RestHenleggBehandlingInfo> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: RestHenleggBehandlingInfo): StegType {
        if (data.årsak == HenleggÅrsak.SØKNAD_TRUKKET) {
            dokumentService.sendManueltBrev(
                behandling = behandling,
                fagsakId = behandling.fagsak.id,
                manueltBrevRequest = ManueltBrevRequest(
                    mottakerIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                    brevmal = BrevType.HENLEGGE_TRUKKET_SØKNAD,
                ).byggMottakerdata(behandling, persongrunnlagService, arbeidsfordelingService)
            )
        }

        oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling).forEach {
            oppgaveService.ferdigstillOppgave(behandling.id, it.type)
        }

        loggService.opprettHenleggBehandling(behandling, data.årsak.beskrivelse, data.begrunnelse)

        behandling.resultat = data.årsak.tilBehandlingsresultat()
        behandling.leggTilHenleggStegOmDetIkkeFinnesFraFør()

        behandlingService.lagreEllerOppdater(behandling)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.HENLEGG_BEHANDLING
    }
}
