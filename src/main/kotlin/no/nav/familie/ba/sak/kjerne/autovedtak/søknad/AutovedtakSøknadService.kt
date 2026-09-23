package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService.Companion.logger
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
    private val stegService: StegService,
    private val oppgaveService: OppgaveService,
) : AutovedtakBehandlingService<SøknadData> {
    override fun skalAutovedtakBehandles(behandlingsdata: SøknadData): Boolean = true

    override fun kjørBehandling(behandlingsdata: SøknadData): String {
        val behandling =
            stegService.håndterNyBehandling(
                behandlingsdata.nyBehandling.copy(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
                    skalBehandlesAutomatisk = true
                ),
            )

        return try {
            vedtaAutomatisk(behandling = behandling, behandlingsdata = behandlingsdata)
        } catch (feil: AutovedtakMåBehandlesManueltFeil) {
            // Kaster ikke videre fordi henleggelsen må committes sammen med resten av transaksjonen.
            henleggBehandlingOgOpprettManuellBehandling(
                automatiskBehandling = behandling,
                behandlingsdata = behandlingsdata,
                begrunnelse = feil.beskrivelse,
            )
        }
    }

    private fun vedtaAutomatisk(
        behandling: Behandling,
        behandlingsdata: SøknadData,
    ): String {
        val behandlingEtterFiltrering =
            stegService.håndterFiltreringsreglerForAutomatiskeBehandlinger(
                behandling,
                FiltrerAutomatiskBehandlingData(
                    søkersIdent = behandlingsdata.søkersIdent,
                    barnasIdenter = behandlingsdata.nyBehandling.barnasIdenter,
                ),
            )

        if (behandlingEtterFiltrering.steg == StegType.HENLEGG_BEHANDLING) {
            logger.info("Filtreringsreglene stoppet den automatiske behandlingen ${behandlingEtterFiltrering.id}")
            throw AutovedtakMåBehandlesManueltFeil("Filtreringsreglene stoppet den automatiske behandlingen ${behandlingEtterFiltrering.id}")
        }

        val automatiskBehandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandlingEtterFiltrering)

        val automatiskBehandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(automatiskBehandlingEtterVilkårsvurdering)

        // Vi kjører ikke simuleringssteget så må gjøre det manuelt her
        val simulering = simuleringService.oppdaterSimuleringPåBehandling(automatiskBehandlingEtterBehandlingsresultat)
        autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering)

        if (automatiskBehandlingEtterBehandlingsresultat.steg != StegType.IVERKSETT_MOT_OPPDRAG) {
            throw Feil("Ugyldig neste steg ${automatiskBehandlingEtterBehandlingsresultat.steg} for behandlingsårsak ${BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD} for fagsak=${behandlingsdata.nyBehandling.fagsakId}")
        }

        autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(automatiskBehandlingEtterBehandlingsresultat)

        val opprettetVedtak = autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(automatiskBehandlingEtterBehandlingsresultat)

        taskService.save(
            IverksettMotOppdragTask.opprettTask(
                automatiskBehandlingEtterBehandlingsresultat,
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
