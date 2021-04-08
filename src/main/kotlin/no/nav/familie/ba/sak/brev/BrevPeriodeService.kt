package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val featureToggleService: FeatureToggleService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    fun hentBrevPerioder(vedtak: Vedtak): List<BrevPeriode> {
        val visOpphørsperioder = featureToggleService.isEnabled(FeatureToggleConfig.VIS_OPPHØRSPERIODER_TOGGLE)
        val sorterteVedtaksperioder =
                sorterVedtaksperioderForBrev(alleVedtaksperioder = vedtaksperiodeService.hentVedtaksperioder(vedtak.behandling))

        val avslagsBegrunnelser = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(
                vedtak.behandling.id)?.let {
            VedtakService.mapAvslagBegrunnelser(
                    avslagBegrunnelser = vedtak.vedtakBegrunnelser
                            .filter { it.begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG }.toList(),
                    personopplysningGrunnlag = it)
        } ?: error("Finner ikke aktivt personopplysningsgrunnlag")

        return vedtaksperioderTilBrevPerioder(sorterteVedtaksperioder,
                                              visOpphørsperioder,
                                              vedtak.vedtakBegrunnelser,
                                              avslagsBegrunnelser).reversed()
    }

    companion object {

        fun sorterVedtaksperioderForBrev(alleVedtaksperioder: List<Vedtaksperiode>): List<Vedtaksperiode> {
            val (perioderMedFom, perioderUtenFom) = alleVedtaksperioder.partition { it.periodeFom != null }
            return perioderMedFom.sortedBy { it.periodeFom } + perioderUtenFom
        }
    }
}