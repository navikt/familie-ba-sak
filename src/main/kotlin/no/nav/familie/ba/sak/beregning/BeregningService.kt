package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonYtelsesperiode
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonRepository
import org.springframework.stereotype.Service

@Service
class BeregningService(
        private val vedtakPersonRepository: VedtakPersonRepository
) {

    fun hentPersonerForVedtak(vedtakId: Long): List<VedtakPersonYtelsesperiode> {
        return vedtakPersonRepository.finnPersonBeregningForVedtak(vedtakId)
    }
}