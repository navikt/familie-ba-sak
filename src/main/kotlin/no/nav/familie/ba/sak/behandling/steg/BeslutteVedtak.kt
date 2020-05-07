package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.ToTrinnKontrollService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillOppgaveTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeslutteVedtak(
        private val toTrinnKontrollService: ToTrinnKontrollService,
        private val vedtakService: VedtakService,
        private val taskRepository: TaskRepository,
        private val loggService: LoggService
) : BehandlingSteg<RestBeslutningPåVedtak> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: RestBeslutningPåVedtak): StegType {
        if (behandling.status == BehandlingStatus.SENDT_TIL_IVERKSETTING) {
            error("Behandlingen er allerede sendt til oppdrag og venter på kvittering")
        } else if (behandling.status == BehandlingStatus.IVERKSATT ||
                   behandling.status == BehandlingStatus.FERDIGSTILT) {
            error("Behandlingen er allerede iverksatt/ferdigstilt")
        }

        if (behandling.status != BehandlingStatus.SENDT_TIL_BESLUTTER) {
            error("Kan ikke beslutte et vedtak som ikke er foreslått av en saksbehandler")
        }

        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
        toTrinnKontrollService.valider2trinnVedBeslutningOmIverksetting(behandling, saksbehandlerId, data.beslutning)

        if (data.beslutning.erGodkjent()) {
            val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                         ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

            vedtakService.godkjennVedtak(vedtak)

            opprettTaskIverksettMotOppdrag(behandling, vedtak, saksbehandlerId)
        }

        loggService.opprettBeslutningOmVedtakLogg(behandling, data.beslutning, saksbehandlerId, data.begrunnelse)
        val ferdigstillGodkjenneVedtakTask = FerdigstillOppgaveTask.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak)
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
            StegType.REGISTRERE_SØKNAD
        }
    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    private fun opprettTaskIverksettMotOppdrag(behandling: Behandling, vedtak: Vedtak, saksbehandlerId: String) {
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, saksbehandlerId)
        taskRepository.save(task)
    }
}