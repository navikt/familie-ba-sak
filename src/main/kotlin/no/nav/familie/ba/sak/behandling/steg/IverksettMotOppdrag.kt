package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdrag(private val økonomiService: ØkonomiService,
                          private val totrinnskontrollService: TotrinnskontrollService,
                          private val infotrygdFeedClient: InfotrygdFeedClient,
                          private val vedtakService: VedtakService
                          ) : BehandlingSteg<IverksettingTaskDTO> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        val vilkårsvurdering: Vilkårsvurdering = stegService?.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering
        vilkårsvurdering.postValiderSteg(behandling)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)
                               ?: throw Feil(message = "Mangler totrinnskontroll ved iverksetting",
                                             frontendFeilmelding = "Mangler totrinnskontroll ved iverksetting"
                               )

        if (totrinnskontroll.erUgyldig()) {
            throw Feil(message = "Totrinnskontroll($totrinnskontroll) er ugyldig ved iverksetting",
                       frontendFeilmelding = "Totrinnskontroll er ugyldig ved iverksetting")
        }

        if (!totrinnskontroll.godkjent) {
            throw Feil(message = "Prøver å iverksette et underkjent vedtak",
                       frontendFeilmelding = ""
            )
        }
    }

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: IverksettingTaskDTO): StegType {
        infotrygdFeedClient.sendVedtakFeedTilInfotrygd(InfotrygdVedtakFeedDto(
                hentFnrStoenadsmottaker(behandling.fagsak),
                hentVedtaksdato(behandling.id)))

        økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(data.vedtaksId,
                                                               data.saksbehandlerId)

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