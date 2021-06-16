package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.BrevPeriode
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