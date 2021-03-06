package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeslutteVedtak(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val vedtakService: VedtakService,
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService,
        private val vilkårsvurderingService: VilkårsvurderingService
) : BehandlingSteg<RestBeslutningPåVedtak> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: RestBeslutningPåVedtak): StegType {
        if (behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK) {
            error("Behandlingen er allerede sendt til oppdrag og venter på kvittering")
        } else if (behandling.status == BehandlingStatus.AVSLUTTET) {
            error("Behandlingen er allerede avsluttet")
        }

        totrinnskontrollService.besluttTotrinnskontroll(behandling = behandling,
                                                        beslutter = SikkerhetContext.hentSaksbehandlerNavn(),
                                                        beslutterId = SikkerhetContext.hentSaksbehandler(),
                                                        beslutning = data.beslutning)

        return if (data.beslutning.erGodkjent()) {
            val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                         ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

            val nesteSteg = hentNesteStegForNormalFlyt(behandling)

            vedtakService.oppdaterVedtaksdatoOgBrev(vedtak)

            when (nesteSteg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> opprettTaskIverksettMotOppdrag(behandling, vedtak)
                StegType.JOURNALFØR_VEDTAKSBREV -> opprettJournalførVedtaksbrevTask(behandling, vedtak)
                else -> throw Feil("Neste steg '$nesteSteg' er ikke implementert på beslutte vedtak steg")
            }

            opprettTaskFerdigstillGodkjenneVedtak(behandling = behandling, beslutning = data)

            nesteSteg
        } else {
            val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
                                   ?: throw Feil("Fant ikke vilkårsvurdering på behandling")
            val kopiertVilkårsVurdering = vilkårsvurdering.kopier(inkluderAndreVurderinger = true)
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = kopiertVilkårsVurdering)

            behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling,
                                                                      kopierVedtakBegrunnelser = true,
                                                                      begrunnelseVilkårPekere =
                                                                      VilkårsvurderingService.matchVilkårResultater(
                                                                              vilkårsvurdering,
                                                                              kopiertVilkårsVurdering))

            opprettTaskFerdigstillGodkjenneVedtak(behandling = behandling, beslutning = data)

            val behandleUnderkjentVedtakTask = OpprettOppgaveTask.opprettTask(
                    behandlingId = behandling.id,
                    oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                    fristForFerdigstillelse = LocalDate.now()
            )
            taskRepository.save(behandleUnderkjentVedtakTask)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    private fun opprettTaskFerdigstillGodkjenneVedtak(behandling: Behandling, beslutning: RestBeslutningPåVedtak) {
        loggService.opprettBeslutningOmVedtakLogg(behandling, beslutning.beslutning, beslutning.begrunnelse)
        val ferdigstillGodkjenneVedtakTask = FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak)
        taskRepository.save(ferdigstillGodkjenneVedtakTask)
    }

    private fun opprettTaskIverksettMotOppdrag(behandling: Behandling, vedtak: Vedtak) {
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }

    private fun opprettJournalførVedtaksbrevTask(behandling: Behandling, vedtak: Vedtak) {
        val task = JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                vedtakId = vedtak.id,
                personIdent = behandling.fagsak.hentAktivIdent().ident,
                behandlingId = behandling.id
        )
        taskRepository.save(task)
    }
}