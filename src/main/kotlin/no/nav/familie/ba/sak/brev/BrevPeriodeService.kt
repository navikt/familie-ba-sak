package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.config.FeatureToggleService
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val featureToggleService: FeatureToggleService
) {

    fun hentBrevPerioder(vedtak: Vedtak): List<BrevPeriode> {
        val vedtaksperioder = vedtaksperiodeService.hentVedtaksperioder(vedtak.behandling)
        val visOpphørsperioder = featureToggleService.isEnabled("familie-ba-sak.behandling.vis-opphoersperioder")
        val sorterteVedtaksperioder = vedtaksperioder.sortedBy { it.periodeFom }.reversed()
        return vedtaksperioderTilBrevPerioder(sorterteVedtaksperioder, visOpphørsperioder, vedtak)
    }
}