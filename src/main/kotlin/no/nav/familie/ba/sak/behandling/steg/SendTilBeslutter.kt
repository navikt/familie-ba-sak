package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
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
        private val vilkårsvurderingService: VilkårsvurderingService
) : BehandlingSteg<String> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {

        vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
                ?.personResultater
                ?.flatMap { it.andreVurderinger }
                ?.takeIf { it.any { annenVurdering -> annenVurdering.resultat == Resultat.IKKE_VURDERT } }
                ?.let {
                    throw FunksjonellFeil(
                            melding = "Forsøker å ferdigstille uten å ha fylt ut påkrevde vurderinger",
                            frontendFeilmelding = "Andre vurderinger må tas stilling til før behandling kan sendes til beslutter.")
                }

        val vilkårsvurderingSteg: VilkårsvurderingSteg =
                stegService?.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as VilkårsvurderingSteg

        vilkårsvurderingSteg.postValiderSteg(behandling)

        behandling.validerRekkefølgeOgUnikhetPåSteg()
        behandling.validerMaksimaltEtStegIkkeUtført()
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

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
                               ?: throw Feil("Fant ikke vilkårsvurdering på behandling")

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering.kopier())

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }
}

fun Behandling.validerRekkefølgeOgUnikhetPåSteg(): Boolean {
    if (erHenlagt()) {
        throw Feil("Valideringen kan ikke kjøres for henlagte behandlinger.")
    }

    var forrigeBehandlingStegTilstand: BehandlingStegTilstand? = null
    behandlingStegTilstand.forEach {
        if (forrigeBehandlingStegTilstand != null
            && forrigeBehandlingStegTilstand!!.behandlingSteg >= it.behandlingSteg
            && (forrigeBehandlingStegTilstand!!.behandlingSteg.rekkefølge != it.behandlingSteg.rekkefølge ||
                forrigeBehandlingStegTilstand!!.behandlingSteg == it.behandlingSteg)) {
            throw Feil("Rekkefølge på steg registrert på behandling ${id} er feil eller redundante.")
        }
        forrigeBehandlingStegTilstand = it
    }
    return true
}

fun Behandling.validerMaksimaltEtStegIkkeUtført() {
    if (erHenlagt()) {
        throw Feil("Valideringen kan ikke kjøres for henlagte behandlinger.")
    }

    if (behandlingStegTilstand.filter { it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT }.size > 1) {
        throw Feil("Behandling ${id} har mer enn ett ikke fullført steg.")
    }
}