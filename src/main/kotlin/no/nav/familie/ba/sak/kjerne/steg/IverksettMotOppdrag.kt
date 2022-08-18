package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.SendVedtakTilInfotrygdTask
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdrag(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val økonomiService: ØkonomiService,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val featureToggleService: FeatureToggleService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService
) : BehandlingSteg<IverksettingTaskDTO> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)

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
        val utbetalingsoppdrag = økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtakService.hent(data.vedtaksId),
            saksbehandlerId = data.saksbehandlerId
        )
        secureLogger.info("Generert utbetalingsoppdrag under iverksettelse på gamle måte=$utbetalingsoppdrag")
        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_GENERERE_UTBETALINGSOPPDRAG_NY)) {
            val generertUtbetalingsoppdrag = utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
                vedtak = vedtakService.hent(data.vedtaksId),
                saksbehandlerId = data.saksbehandlerId
            )
            secureLogger.info("Generert utbetalingsoppdrag under iverksettelse på ny måte=$generertUtbetalingsoppdrag")
        }
        val forrigeIverksatteBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
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
