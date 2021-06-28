package no.nav.familie.ba.sak.kjerne.autorevurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autobrev.Autobrev6og18ÅrService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SatsendringService(private val stegService: StegService,
                         private val vedtakService: VedtakService,
                         private val taskRepository: TaskRepository) {


    // TODO: Behandlinger plukkes i egen jobb. Ytelse?
    fun revurderOgopprettiverksettingstask(behandling: Behandling) {

        logger.info("revurderer og oppretter iverksetting på behandling ${behandling.id}")

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å revurdere sats på ikke løpende fagsak ${behandling.fagsak.id}")
        if (behandling.status != BehandlingStatus.AVSLUTTET) throw Feil("Forsøker å revurdere sats på behandling som ikke er avsluttet ${behandling.id} ")

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

        val task = IverksettMotOppdragTask.opprettTask(behandling, opprettetVedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SatsendringService::class.java)
    }
}