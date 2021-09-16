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

    companion object {

        fun sorterVedtaksperioderForBrev(alleVedtaksperioder: List<Vedtaksperiode>): List<Vedtaksperiode> {
            val (perioderMedFom, perioderUtenFom) = alleVedtaksperioder.partition { it.periodeFom != null }
            return perioderMedFom.sortedBy { it.periodeFom } + perioderUtenFom
        }
    }
}