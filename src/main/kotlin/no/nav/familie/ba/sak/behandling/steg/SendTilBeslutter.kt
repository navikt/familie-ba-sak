package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.OpprettGodkjenneVedtakOppgaveTilBeslutter
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class SendTilBeslutter(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository,
        private val oppgaveRepository: OppgaveRepository,
        private val loggService: LoggService
) : BehandlingSteg<String> {

    override fun utførSteg(behandling: Behandling, data: String): Behandling {
        loggService.opprettSendTilBeslutterLogg(behandling)
        val godkjenneVedtakTask = Task.nyTask(OpprettGodkjenneVedtakOppgaveTilBeslutter.TASK_STEP_TYPE, behandling.id.toString())
        taskRepository.save(godkjenneVedtakTask)

        val behandleSakOppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.BehandleSak, behandling)
        if (behandleSakOppgave !== null) {
            val ferdigstillBehandleSakTask = FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.BehandleSak)
            taskRepository.save(ferdigstillBehandleSakTask)
        }

        val behandleUnderkjentVedtakOppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.BehandleUnderkjentVedtak, behandling)
        if (behandleUnderkjentVedtakOppgave !== null) {
            val ferdigstillBehandleUnderkjentVedtakTask = FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.BehandleUnderkjentVedtak)
            taskRepository.save(ferdigstillBehandleUnderkjentVedtakTask)
        }
        return behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id,
                                                            status = BehandlingStatus.SENDT_TIL_BESLUTTER)
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }
}