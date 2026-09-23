package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SøknadData
import no.nav.familie.ba.sak.kjerne.behandling.HenleggBehandlingInfoDto
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service

@Service
class AutovedtakSøknadService(
    private val autovedtakService: AutovedtakService,
    private val simuleringService: SimuleringService,
    private val taskService: TaskService,
    private val autovedtakSøknadBegrunnelseService: AutovedtakSøknadBegrunnelseService,
    private val autovedtakSøknadValideringService: AutovedtakSøknadValideringService,
    private val filtreringsreglerSøknadService: FiltreringsreglerSøknadService,
    private val stegService: StegService,
    private val oppgaveService: OppgaveService,
) : AutovedtakBehandlingService<SøknadData> {
    override fun skalAutovedtakBehandles(behandlingsdata: SøknadData): Boolean = true

    override fun kjørBehandling(behandlingsdata: SøknadData): String {
        val automatiskBehandling =
            autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                nyBehandling =
                    behandlingsdata.nyBehandling.copy(
                        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                        behandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
                    ),
                filtrerAutomatiskBehandlingData =
                    FiltrerAutomatiskBehandlingData(
                        søkersIdent = behandlingsdata.søkersIdent,
                        barnasIdenter = behandlingsdata.nyBehandling.barnasIdenter,
                    ),
            )

        if (automatiskBehandling.steg == StegType.HENLEGG_BEHANDLING) {
            return henleggBehandlingOgOpprettManuellBehandling(
                automatiskBehandling = automatiskBehandling,
                behandlingsdata = behandlingsdata,
                begrunnelse = filtreringsreglerSøknadService.hentBegrunnelseForIkkeOppfyltFiltreringsregel(behandlingId = automatiskBehandling.id),
            )
        }

        return try {
            vedtaAutomatisk(behandling = automatiskBehandling, behandlingsdata = behandlingsdata)
        } catch (feil: AutovedtakMåBehandlesManueltFeil) {
            // Kaster ikke videre fordi henleggelsen må committes sammen med resten av transaksjonen.
            henleggBehandlingOgOpprettManuellBehandling(
                automatiskBehandling = automatiskBehandling,
                behandlingsdata = behandlingsdata,
                begrunnelse = feil.beskrivelse,
            )
        }
    }

    private fun vedtaAutomatisk(
        behandling: Behandling,
        behandlingsdata: SøknadData,
    ): String {
        // Vi kjører ikke simuleringssteget så må gjøre det manuelt her
        val simulering = simuleringService.oppdaterSimuleringPåBehandling(behandling)
        autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering)

        if (behandling.steg != StegType.IVERKSETT_MOT_OPPDRAG) {
            throw Feil("Ugyldig neste steg ${behandling.steg} for behandlingsårsak ${BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD} for fagsak=${behandlingsdata.nyBehandling.fagsakId}")
        }

        autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(behandling)

        val opprettetVedtak = autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling)

        taskService.save(
            IverksettMotOppdragTask.opprettTask(
                behandling,
                opprettetVedtak,
                SikkerhetContext.hentSaksbehandler(),
            ),
        )

        return AutovedtakStegService.BEHANDLING_FERDIG
    }

    private fun henleggBehandlingOgOpprettManuellBehandling(
        automatiskBehandling: Behandling,
        behandlingsdata: SøknadData,
        begrunnelse: String,
    ): String {
        stegService.håndterHenleggBehandling(
            behandling = automatiskBehandling,
            henleggBehandlingInfo =
                HenleggBehandlingInfoDto(
                    årsak = HenleggÅrsak.AUTOMATISK_HENLAGT,
                    begrunnelse = begrunnelse,
                ),
        )

        val manuellBehandling =
            stegService.håndterNyBehandlingOgSendInfotrygdFeed(
                behandlingsdata.nyBehandling.copy(behandlingÅrsak = BehandlingÅrsak.SØKNAD),
            )

        oppgaveService.opprettOppgaveForManuellBehandling(
            behandlingId = manuellBehandling.id,
            begrunnelse = begrunnelse,
            manuellOppgaveType = ManuellOppgaveType.SØKNAD,
            oppgavetype = Oppgavetype.BehandleSak,
        )

        return "Automatisk behandling av søknad er henlagt og sendt til manuell behandling: $begrunnelse"
    }
}
