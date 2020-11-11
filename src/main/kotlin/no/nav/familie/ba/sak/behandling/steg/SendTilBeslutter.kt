package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktService
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktStatus
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
        private val opplysningspliktService: OpplysningspliktService,
        private val behandlingResultatService: BehandlingResultatService
) : BehandlingSteg<String> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        val opplysningsplikt = opplysningspliktService.hent(behandlingId = behandling.id);
        if (opplysningsplikt !== null && opplysningsplikt.status == OpplysningspliktStatus.IKKE_SATT) {
            throw FunksjonellFeil(
                    melding = "Forsøker å ferdigstille uten å ha fylt ut påkrevd opplysningsplikt",
                    frontendFeilmelding = "Opplysningsplikt må tas stilling til før behandling kan sendes til beslutter.")
        }

        val vilkårsvurdering: Vilkårsvurdering = stegService?.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering
        vilkårsvurdering.postValiderSteg(behandling)

        var forrigeBehandlingStegTilstand: BehandlingStegTilstand? = null
        //Todo: sortedBy skal muligens erstattes av annen løsning
        behandling.behandlingStegTilstand.sortedBy { it.opprettetTidspunkt }.forEach {
            if (forrigeBehandlingStegTilstand != null && forrigeBehandlingStegTilstand!!.behandlingSteg >= it.behandlingSteg) {
                throw Feil("Rekkefølge på steg registrert på behandling ${behandling.id} er feil eller redundante.")
            }
            forrigeBehandlingStegTilstand = it
        }
        if (behandling.behandlingStegTilstand.filter { it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT }.size > 1) {
            throw Feil("Behandling ${behandling.id} har mer enn ett ikke fullført steg.")
        }
    }

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        loggService.opprettSendTilBeslutterLogg(behandling)
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling)

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
        behandlingService.sendBehandlingTilBeslutter(behandling)

        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)
                                 ?: throw Feil("Fant ikke behandlingsresultat på behandling")

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat.kopier())

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }
}