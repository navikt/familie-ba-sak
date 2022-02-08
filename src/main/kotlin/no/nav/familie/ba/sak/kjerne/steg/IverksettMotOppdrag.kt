package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.SendVedtakTilInfotrygdTask
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdrag(
    private val økonomiService: ØkonomiService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
) : BehandlingSteg<IverksettingTaskDTO> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        val behandlingsresultatSteg: BehandlingsresultatSteg =
            stegService?.hentBehandlingSteg(StegType.BEHANDLINGSRESULTAT) as BehandlingsresultatSteg
        behandlingsresultatSteg.preValiderSteg(behandling)

        val beslutteVedtakSteg: BeslutteVedtak = stegService.hentBehandlingSteg(StegType.BESLUTTE_VEDTAK) as BeslutteVedtak
        beslutteVedtakSteg.postValiderSteg(behandling)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)
            ?: throw Feil(
                message = "Mangler totrinnskontroll ved iverksetting",
                frontendFeilmelding = "Mangler totrinnskontroll ved iverksetting"
            )

        if (totrinnskontroll.erUgyldig()) {
            throw Feil(
                message = "Totrinnskontroll($totrinnskontroll) er ugyldig ved iverksetting",
                frontendFeilmelding = "Totrinnskontroll er ugyldig ved iverksetting"
            )
        }

        if (!totrinnskontroll.godkjent) {
            throw Feil(
                message = "Prøver å iverksette et underkjent vedtak",
                frontendFeilmelding = ""
            )
        }
    }

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: IverksettingTaskDTO
    ): StegType {

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtakService.hent(data.vedtaksId),
            saksbehandlerId = data.saksbehandlerId
        )

        val forrigeIverksatteBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling)
        if (forrigeIverksatteBehandling == null ||
            forrigeIverksatteBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
        ) {
            taskRepository.save(
                SendVedtakTilInfotrygdTask.opprettTask(
                    hentFnrStoenadsmottaker(behandling.fagsak),
                    behandling.id
                )
            )
        }
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.IVERKSETT_MOT_OPPDRAG
    }

    private fun hentFnrStoenadsmottaker(fagsak: Fagsak) = fagsak.aktør.aktivFødselsnummer()
}
