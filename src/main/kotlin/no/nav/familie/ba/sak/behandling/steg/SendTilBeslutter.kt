package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SendTilBeslutter(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val oppgaveRepository: OppgaveRepository,
        private val vedtakService: VedtakService,
        private val loggService: LoggService
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String,
                                      stegService: StegService?): StegType {
        val vilkårsvurdering: Vilkårsvurdering = stegService?.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering
        vilkårsvurdering.validerSteg(behandling)

        loggService.opprettSendTilBeslutterLogg(behandling)
        val godkjenneVedtakTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                fristForFerdigstillelse = LocalDate.now()
        )
        taskRepository.save(godkjenneVedtakTask)

        val behandleSakOppgave =
                oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.BehandleSak, behandling)
        if (behandleSakOppgave !== null) {
            val ferdigstillBehandleSakTask = FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.BehandleSak)
            taskRepository.save(ferdigstillBehandleSakTask)
        }

        val behandleUnderkjentVedtakOppgave =
                oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.BehandleUnderkjentVedtak,
                                                                                   behandling)
        if (behandleUnderkjentVedtakOppgave !== null) {
            val ferdigstillBehandleUnderkjentVedtakTask =
                    FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.BehandleUnderkjentVedtak)
            taskRepository.save(ferdigstillBehandleUnderkjentVedtakTask)
        }
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id,
                                                     status = BehandlingStatus.SENDT_TIL_BESLUTTER)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
            ?: error("Fant ikke foreslått vedtak på behandling ${behandling.id}")
        vedtak.ansvarligEnhet = data
        vedtakService.lagreEllerOppdater(vedtak)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }
}