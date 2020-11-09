package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeslutteVedtak(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val vedtakService: VedtakService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService
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
                                                        beslutning = data.beslutning)

        if (data.beslutning.erGodkjent()) {
            val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                         ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

            vedtakService.oppdaterVedtaksdatoOgBrev(vedtak)

            opprettTaskIverksettMotOppdrag(behandling, vedtak)
        }

        loggService.opprettBeslutningOmVedtakLogg(behandling, data.beslutning, data.begrunnelse)
        val ferdigstillGodkjenneVedtakTask = FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak)
        taskRepository.save(ferdigstillGodkjenneVedtakTask)

        return if (data.beslutning.erGodkjent()) {
            hentNesteStegForNormalFlyt(behandling)
        } else {
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

    private fun opprettTaskIverksettMotOppdrag(behandling: Behandling, vedtak: Vedtak) {
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }
}