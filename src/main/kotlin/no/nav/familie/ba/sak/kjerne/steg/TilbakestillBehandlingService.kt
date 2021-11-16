package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingService(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val beregningService: BeregningService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakRepository: VedtakRepository,
    private val tilbakekrevingService: TilbakekrevingService
) {

    @Transactional
    fun initierOgSettBehandlingTilVilårsvurdering(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean = true
    ) {
        vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = bekreftEndringerViaFrontend,
            forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(
                behandling
            )
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
            ?: error("Fant ikke aktivt vedtak for behandling")

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.id)
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtak)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.id,
            steg = StegType.VILKÅRSVURDERING
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandling.id)

        vedtakRepository.saveAndFlush(vedtak)
    }
}
