package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
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
        private val vedtakService: VedtakService,
) {

    @Transactional
    fun initierOgSettBehandlingTilVilårsvurdering(behandling: Behandling, bekreftEndringerViaFrontend: Boolean = true) {

        val forrigeBehandlingSomErIverksatt = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
        vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                           bekreftEndringerViaFrontend = bekreftEndringerViaFrontend,
                                                           forrigeBehandling = forrigeBehandlingSomErIverksatt)

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.id)
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtak)
        vedtak.settBegrunnelser(emptySet())

        vedtakService.settStegSlettVedtakBegrunnelserOgTilbakekreving(behandlingId = behandling.id)

        vedtakService.oppdater(vedtak)
    }
}

