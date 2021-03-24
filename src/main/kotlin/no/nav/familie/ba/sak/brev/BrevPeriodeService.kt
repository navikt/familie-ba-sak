package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val featureToggleService: FeatureToggleService
) {

    fun hentBrevPerioder(vedtak: Vedtak): List<BrevPeriode> {
        val (avslagPerioder, øvrigeVedtaksperioder) = vedtaksperiodeService.hentVedtaksperioder(vedtak.behandling)
                .partition { it.vedtaksperiodetype == Vedtaksperiodetype.AVSLAG }
        val (avslagsPerioderUtenBegrensetPeriode, øvrigeAvslagsperioder) = avslagPerioder
                .partition { it.periodeTom == null }

        val visOpphørsperioder = featureToggleService.isEnabled(FeatureToggleConfig.VIS_OPPHØRSPERIODER_TOGGLE)
        val sorterteVedtaksperioder = if (featureToggleService.isEnabled(FeatureToggleConfig.VIS_AVSLAG_TOGGLE)) {
            (øvrigeVedtaksperioder + øvrigeAvslagsperioder).sortedBy { it.periodeFom }.reversed() +
            avslagsPerioderUtenBegrensetPeriode
        } else øvrigeVedtaksperioder.sortedBy { it.periodeFom }.reversed()

        return vedtaksperioderTilBrevPerioder(sorterteVedtaksperioder, visOpphørsperioder, vedtak)
    }
}