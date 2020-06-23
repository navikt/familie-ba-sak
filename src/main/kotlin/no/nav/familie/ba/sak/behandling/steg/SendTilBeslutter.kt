package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SendTilBeslutter(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val oppgaveService: OppgaveService,
        private val loggService: LoggService,
        private val totrinnskontrollService: TotrinnskontrollService,
        private val behandlingResultatService: BehandlingResultatService
) : BehandlingSteg<String> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        val vilkårsvurdering: Vilkårsvurdering = stegService?.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering
        vilkårsvurdering.postValiderSteg(behandling)
    }

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        loggService.opprettSendTilBeslutterLogg(behandling)
        totrinnskontrollService.opprettEllerHentTotrinnskontroll(behandling)

        val godkjenneVedtakTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                fristForFerdigstillelse = LocalDate.now()
        )
        taskRepository.save(godkjenneVedtakTask)

        val behandleSakDbOppgave =
                oppgaveService.hentOppgaveSomIkkeErFerdigstilt(Oppgavetype.BehandleSak, behandling)
        if (behandleSakDbOppgave !== null) {
            val ferdigstillBehandleSakTask = FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.BehandleSak)
            taskRepository.save(ferdigstillBehandleSakTask)
        }

        val behandleUnderkjentVedtakOppgave =
                oppgaveService.hentOppgaveSomIkkeErFerdigstilt(Oppgavetype.BehandleUnderkjentVedtak,
                                                               behandling)
        if (behandleUnderkjentVedtakOppgave !== null) {
            val ferdigstillBehandleUnderkjentVedtakTask =
                    FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.BehandleUnderkjentVedtak)
            taskRepository.save(ferdigstillBehandleUnderkjentVedtakTask)
        }
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id,
                                                     status = BehandlingStatus.SENDT_TIL_BESLUTTER)

        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)?:
                                 throw Feil("Fant ikke behandlingsresultat på behandling")

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat.kopier(),
                                                           false)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }
}