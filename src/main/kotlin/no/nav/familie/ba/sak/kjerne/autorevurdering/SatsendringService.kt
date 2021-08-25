package no.nav.familie.ba.sak.kjerne.autorevurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class SatsendringService(
        private val stegService: StegService,
        private val vedtakService: VedtakService,
        private val taskRepository: TaskRepository,
        private val behandlingRepository: BehandlingRepository,
) {

    /**
     * Finner behandlinger som trenger satsendring.
     *
     * Obs! Denne utplukkingen tar også med inaktive behandlinger, siden den aktive behandlingen ikke nødvendigvis
     * iverksatte (f.eks. omregning eller henleggelse). Dette betyr at man potensielt får med fagsaker hvor
     * behovet for revurdering i ettertid har blitt fjernet. Dersom man ønsker å filtrere bort disse må
     * man sjekke om den inaktive behandlingen blir etterfulgt av revurdering som fjerner behovet.
     */
    fun finnBehandlingerForSatsendring(gammelSats: Long,
                                       satsendringMåned: YearMonth): List<Long> =
            behandlingRepository.finnBehadlingerForSatsendring(
                    iverksatteLøpende = behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker(),
                    gammelSats = gammelSats,
                    månedÅrForEndring = satsendringMåned)

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     * Dersom man utfører dette på en behandling uten behov for satsendring eller ny sats ikke er lagt inn i systemet enda,
     * ferdigstilles behandlingen uendret (FORTSATT_INNVILGET). I og med at satsendring ikke trigger brev er det ikke kritisk.
     */
    @Transactional
    fun utførSatsendring(behandlingId: Long) {

        val behandling = behandlingRepository.finnBehandling(behandlingId = behandlingId)
        val søkerIdent = behandling.fagsak.hentAktivIdent().ident

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")
        if (behandling.status != BehandlingStatus.AVSLUTTET) throw Feil("Forsøker å utføre satsendring på behandling ${behandling.id} som ikke er avsluttet")

        val opprettetBehandling = stegService.håndterNyBehandling(NyBehandling(
                søkersIdent = søkerIdent,
                behandlingType = BehandlingType.REVURDERING,
                kategori = behandling.kategori,
                underkategori = behandling.underkategori,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                skalBehandlesAutomatisk = true,
        ))

        stegService.håndterVilkårsvurdering(behandling = opprettetBehandling)

        val opprettetVedtak = vedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(opprettetBehandling)

        val task = if (opprettetBehandling.resultat == BehandlingResultat.ENDRET) {
            IverksettMotOppdragTask.opprettTask(opprettetBehandling,
                                                opprettetVedtak,
                                                SikkerhetContext.hentSaksbehandler())
        } else {
            FerdigstillBehandlingTask.opprettTask(personIdent = søkerIdent,
                                                  behandlingsId = opprettetBehandling.id)
        }
        taskRepository.save(task)

    }
}