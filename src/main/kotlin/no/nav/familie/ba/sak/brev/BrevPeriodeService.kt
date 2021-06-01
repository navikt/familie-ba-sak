package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.hentUtbetalingsperiodeForVedtaksperiode
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {

    fun hentBrevPerioder(vedtak: Vedtak): List<BrevPeriode> {
        val sorterteVedtaksperioder =
                sorterVedtaksperioderForBrev(alleVedtaksperioder = vedtaksperiodeService.hentVedtaksperioder(vedtak.behandling))

        val avslagsbegrunnelser =
                personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vedtak.behandling.id)?.let { grunnlag ->
                    VedtakService.mapAvslagBegrunnelser(
                            avslagBegrunnelser = vedtak.vedtakBegrunnelser
                                    .filter { it.begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG }.toList(),
                            personopplysningGrunnlag = grunnlag)
                } ?: error("Finner ikke aktivt personopplysningsgrunnlag")

        return vedtaksperioderTilBrevPerioder(sorterteVedtaksperioder,
                                              vedtak.vedtakBegrunnelser,
                                              avslagsbegrunnelser).reversed()
    }

    companion object {

        fun sorterVedtaksperioderForBrev(alleVedtaksperioder: List<Vedtaksperiode>): List<Vedtaksperiode> {
            val (perioderMedFom, perioderUtenFom) = alleVedtaksperioder.partition { it.periodeFom != null }
            return perioderMedFom.sortedBy { it.periodeFom } + perioderUtenFom
        }
    }
}