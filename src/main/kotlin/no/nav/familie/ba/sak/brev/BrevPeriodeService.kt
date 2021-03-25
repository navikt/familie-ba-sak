package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val featureToggleService: FeatureToggleService
) {

    fun hentBrevPerioder(vedtak: Vedtak): List<BrevPeriode> {
        val visOpphørsperioder = featureToggleService.isEnabled(FeatureToggleConfig.VIS_OPPHØRSPERIODER_TOGGLE)
        val sorterteVedtaksperioder =
                sorterVedtaksperioderForBrev(alleVedtaksperioder = vedtaksperiodeService.hentVedtaksperioder(vedtak.behandling),
                                             visAvslag = featureToggleService.isEnabled(FeatureToggleConfig.VIS_AVSLAG_TOGGLE))

        return vedtaksperioderTilBrevPerioder(sorterteVedtaksperioder, visOpphørsperioder, vedtak).reversed()
    }

    companion object {

        fun sorterVedtaksperioderForBrev(alleVedtaksperioder: List<Vedtaksperiode>,
                                         visAvslag: Boolean = false): List<Vedtaksperiode> {

            return if (visAvslag) {
                val (avslagsPerioderSistIBrevet, øvrigeVedtaksperioder) = alleVedtaksperioder
                        .partition { it.vedtaksperiodetype == Vedtaksperiodetype.AVSLAG && (it.periodeTom == null || it.periodeTom!!.erSenereEnnInneværendeMåned()) }
                val (avslagPerioderHeltUtenDatoer, avslagsPerioderUtenBegrensetPeriode) = avslagsPerioderSistIBrevet
                        .partition { it.periodeFom == null && it.periodeTom == null }

                øvrigeVedtaksperioder.sortedBy { it.periodeFom } +
                avslagsPerioderUtenBegrensetPeriode.sortedBy { it.periodeFom } +
                avslagPerioderHeltUtenDatoer
            } else alleVedtaksperioder.filter { it.vedtaksperiodetype != Vedtaksperiodetype.AVSLAG }
                    .sortedBy { it.periodeFom }

        }
    }
}