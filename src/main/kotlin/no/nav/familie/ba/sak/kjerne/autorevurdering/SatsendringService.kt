package no.nav.familie.ba.sak.kjerne.autorevurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SatsendringService(private val stegService: StegService,
                         private val vedtakService: VedtakService,
                         private val taskRepository: TaskRepository) {


    // TODO: Behandlinger plukkes og kjøres i egen jobb
    @Transactional
    fun utførSatsendring(behandling: Behandling) {

        logger.info("Utfører satsendring på fagsak ${behandling.fagsak.id}")

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")
        if (behandling.status != BehandlingStatus.AVSLUTTET) throw Feil("Forsøker å utføre satsendring på behandling ${behandling.id} som ikke er avsluttet")

        val opprettetBehandling = stegService.håndterNyBehandling(NyBehandling(
                søkersIdent = behandling.fagsak.hentAktivIdent().ident,
                behandlingType = BehandlingType.REVURDERING,
                kategori = behandling.kategori,
                underkategori = behandling.underkategori,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                skalBehandlesAutomatisk = true,
        ))


        stegService.håndterVilkårsvurdering(behandling = opprettetBehandling)

        val opprettetVedtak = vedtakService.opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(opprettetBehandling)

        val task = IverksettMotOppdragTask.opprettTask(opprettetBehandling, opprettetVedtak, SikkerhetContext.hentSaksbehandler())

        taskRepository.save(task)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SatsendringService::class.java)
    }
}