package no.nav.familie.ba.sak.kjerne.steg

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdrag(
    private val økonomiService: ØkonomiService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
) : BehandlingSteg<IverksettingTaskDTO> {
    private val iverksattOppdrag = Metrics.counter("familie.ba.sak.oppdrag.iverksatt")

    override fun preValiderSteg(
        behandling: Behandling,
        stegService: StegService?,
    ) {
        tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)

        val totrinnskontroll =
            totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)
                ?: throw Feil(
                    message = "Mangler totrinnskontroll ved iverksetting",
                    frontendFeilmelding = "Mangler totrinnskontroll ved iverksetting",
                )

        if (totrinnskontroll.erUgyldig()) {
            throw Feil(
                message = "Totrinnskontroll($totrinnskontroll) er ugyldig ved iverksetting",
                frontendFeilmelding = "Totrinnskontroll er ugyldig ved iverksetting",
            )
        }

        if (!totrinnskontroll.godkjent) {
            throw Feil(
                message = "Prøver å iverksette et underkjent vedtak",
                frontendFeilmelding = "",
            )
        }
    }

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: IverksettingTaskDTO,
    ): StegType {
        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtakService.hent(data.vedtaksId),
            saksbehandlerId = data.saksbehandlerId,
        )
        iverksattOppdrag.increment()
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.IVERKSETT_MOT_OPPDRAG
}
