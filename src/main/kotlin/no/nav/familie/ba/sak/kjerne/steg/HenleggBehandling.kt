package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SATSENDRING
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggBehandlingInfoDto
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleSak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleUnderkjentVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.GodkjenneVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.VurderLivshendelse
import org.springframework.stereotype.Service

@Service
class HenleggBehandling(
    private val behandlingService: BehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val loggService: LoggService,
    private val dokumentService: DokumentService,
    private val oppgaveService: OppgaveService,
) : BehandlingSteg<HenleggBehandlingInfoDto> {
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: HenleggBehandlingInfoDto,
    ): StegType {
        val fagsak = behandling.fagsak

        if (data.årsak == HenleggÅrsak.SØKNAD_TRUKKET) {
            val brevmal = fagsak.institusjon?.let { Brevmal.HENLEGGE_TRUKKET_SØKNAD_INSTITUSJON } ?: Brevmal.HENLEGGE_TRUKKET_SØKNAD

            dokumentService.sendManueltBrev(
                behandling = behandling,
                fagsakId = fagsak.id,
                manueltBrevRequest = dokumentService.byggMottakerdataFraBehandling(behandling, ManueltBrevRequest(brevmal)),
            )
        }

        oppgaveService
            .hentOppgaverSomIkkeErFerdigstilt(behandling)
            .filter {
                !(
                    data.årsak == HenleggÅrsak.TEKNISK_VEDLIKEHOLD &&
                        data.begrunnelse == SATSENDRING &&
                        it.type in
                        listOf(
                            BehandleSak,
                            GodkjenneVedtak,
                            BehandleUnderkjentVedtak,
                            VurderLivshendelse,
                        )
                )
            }.forEach {
                oppgaveService.ferdigstillOppgaver(behandling.id, it.type)
            }

        loggService.opprettHenleggBehandling(behandling, data.årsak.beskrivelse, data.begrunnelse)

        behandling.resultat = data.årsak.tilBehandlingsresultat(behandling.opprettetÅrsak)
        behandling.leggTilHenleggStegOmDetIkkeFinnesFraFør()

        behandlingHentOgPersisterService.lagreEllerOppdater(behandling)

        // Slett migreringsdato
        behandlingService.deleteMigreringsdatoVedHenleggelse(behandling.id)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.HENLEGG_BEHANDLING
}
