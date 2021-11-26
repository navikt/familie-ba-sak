package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IverksettMotOppdrag(
    private val økonomiService: ØkonomiService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val infotrygdFeedClient: InfotrygdFeedClient,
    private val vedtakService: VedtakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : BehandlingSteg<IverksettingTaskDTO> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        val behandlingsresultatSteg: BehandlingsresultatSteg =
            stegService?.hentBehandlingSteg(StegType.BEHANDLINGSRESULTAT) as BehandlingsresultatSteg
        behandlingsresultatSteg.preValiderSteg(behandling)

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
        infotrygdFeedClient.sendVedtakFeedTilInfotrygd(
            InfotrygdVedtakFeedDto(
                hentFnrStoenadsmottaker(behandling.fagsak),
                finnFørsteUtbetalingsperiode(behandling.id)
            )
        )

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtakService.hent(data.vedtaksId),
            saksbehandlerId = data.saksbehandlerId
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.IVERKSETT_MOT_OPPDRAG
    }

    private fun hentFnrStoenadsmottaker(fagsak: Fagsak) = fagsak.hentAktivIdent().ident

    private fun finnFørsteUtbetalingsperiode(behandlingId: Long): LocalDate {
        return tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)?.andelerTilkjentYtelse
            ?.let { andelerTilkjentYtelse: MutableSet<AndelTilkjentYtelse> ->
                if (andelerTilkjentYtelse.isNotEmpty()) {
                    val førsteUtbetalingsperiode = beregnUtbetalingsperioderUtenKlassifisering(andelerTilkjentYtelse)
                        .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                        .first()
                    førsteUtbetalingsperiode.fom
                } else null
            } ?: error("Finner ikke første utbetalingsperiode")
    }
}
