package no.nav.familie.ba.sak.behandling.beregning

import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonRepository
import org.springframework.stereotype.Service

@Service
class BeregningService(
        private val vedtakPersonRepository: VedtakPersonRepository
) {

    fun hentPersonerForVedtak(vedtakId: Long?): List<VedtakPerson> {
        return vedtakPersonRepository.finnPersonBeregningForVedtak(vedtakId)
    }
}