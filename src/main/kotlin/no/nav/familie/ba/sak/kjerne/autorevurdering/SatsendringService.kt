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

@Service
class SatsendringService(private val stegService: StegService,
                         private val vedtakService: VedtakService,
                         private val taskRepository: TaskRepository,
                         private val behandlingRepository: BehandlingRepository) {

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

        val opprettetVedtak = vedtakService.opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(opprettetBehandling)

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