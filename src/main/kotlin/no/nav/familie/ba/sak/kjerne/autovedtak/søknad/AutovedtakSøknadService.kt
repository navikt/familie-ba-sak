package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SøknadData
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class AutovedtakSøknadService(
    private val autovedtakService: AutovedtakService,
    private val simuleringService: SimuleringService,
    private val taskService: TaskService,
    private val autovedtakSøknadBegrunnelseService: AutovedtakSøknadBegrunnelseService,
) : AutovedtakBehandlingService<SøknadData> {
    override fun skalAutovedtakBehandles(behandlingsdata: SøknadData): Boolean = true

    override fun kjørBehandling(behandlingsdata: SøknadData): String {
        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                fagsakId = behandlingsdata.søknad.fagsakId,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
                filtrerAutomatiskBehandlingData =
                    FiltrerAutomatiskBehandlingData(
                        søkersIdent = behandlingsdata.søknad.søkersIdent,
                        barnasIdenter = behandlingsdata.søknad.barnasIdenter,
                    ),
            )

        val simulering = simuleringService.oppdaterSimuleringPåBehandling(behandlingEtterBehandlingsresultat)

        val harIngenUtbetaling = simulering.flatMap { it.økonomiSimuleringPostering }.all { it.beløp == BigDecimal.ZERO }
        if (harIngenUtbetaling) {
            throw AutovedtakMåBehandlesManueltFeil("Automatisk behandling av søknad fører til ingen utbetaling.\nEndring av søknad må håndteres manuelt.")
        }

        val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingEtterBehandlingsresultat.id)
        if (feilutbetaling > BigDecimal.ZERO) {
            throw AutovedtakMåBehandlesManueltFeil("Automatisk behandling av søknad fører til feilutbetaling.\nEndring av søknad må håndteres manuelt.")
        }

        if (behandlingEtterBehandlingsresultat.steg == StegType.IVERKSETT_MOT_OPPDRAG) {
            autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad()
        }

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandlingEtterBehandlingsresultat,
                        opprettetVedtak,
                        SikkerhetContext.hentSaksbehandler(),
                    )
                }

                else -> {
                    throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} for behandlingsårsak ${BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD} for fagsak=${behandlingsdata.søknad.fagsakId}")
                }
            }

        taskService.save(task)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }
}
