package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdrag(
    private val økonomiService: ØkonomiService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val infotrygdFeedClient: InfotrygdFeedClient,
    private val vedtakService: VedtakService,
    private val beregningService: BeregningService
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
                hentVedtaksdato(behandling.id).toLocalDate()
            )
        )

        val stringUtbetalingsoppdrag =
            beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id).utbetalingsoppdrag

        val utbetalingsoppdrag =
            if (stringUtbetalingsoppdrag == null) {
                // Etter flyttingen av generering av utbetalingsoppdrag kan det være tasker som er opprettet uten at beregningen er utført.
                // Lager derfor ny beregning dersom utbetalingsoppdraget ikke er generert.
                økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
                    vedtak = vedtakService.hent(data.vedtaksId),
                    saksbehandlerId = data.saksbehandlerId
                )
            } else {
                objectMapper.readValue(stringUtbetalingsoppdrag, Utbetalingsoppdrag::class.java)
            }

        økonomiService.iverksettOppdrag(
            utbetalingsoppdrag = utbetalingsoppdrag
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.IVERKSETT_MOT_OPPDRAG
    }

    private fun hentFnrStoenadsmottaker(fagsak: Fagsak) = fagsak.hentAktivIdent().ident

    private fun hentVedtaksdato(behandlingsId: Long) =
        vedtakService.hentAktivForBehandling(behandlingsId)?.vedtaksdato
            ?: throw Exception("Aktivt vedtak eller vedtaksdato eksisterer ikke for $behandlingsId")
}
