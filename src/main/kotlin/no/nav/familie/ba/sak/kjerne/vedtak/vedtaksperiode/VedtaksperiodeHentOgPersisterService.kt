package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import org.springframework.stereotype.Service

class FantIkkeVedtaksperiodeFeil(vedtaksperiodeId: Long) :
    Feil(
        message = "Fant ingen vedtaksperiode med id $vedtaksperiodeId",
        frontendFeilmelding = "Fant ikke vedtaksperiode"
    )

@Service
class VedtaksperiodeHentOgPersisterService(

    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
) {

    fun hentVedtaksperiodeThrows(vedtaksperiodeId: Long): VedtaksperiodeMedBegrunnelser =
        vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)
            ?: throw FantIkkeVedtaksperiodeFeil(vedtaksperiodeId)

    fun lagre(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): VedtaksperiodeMedBegrunnelser {
        validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser)
        return vedtaksperiodeRepository.save(vedtaksperiodeMedBegrunnelser)
    }

    fun lagre(vedtaksperiodeMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>): List<VedtaksperiodeMedBegrunnelser> {
        vedtaksperiodeMedBegrunnelser.forEach { validerVedtaksperiodeMedBegrunnelser(it) }
        return vedtaksperiodeRepository.saveAll(vedtaksperiodeMedBegrunnelser)
    }

    fun slettVedtaksperioderFor(vedtak: Vedtak) {
        vedtaksperiodeRepository.slettVedtaksperioderFor(vedtak)
    }

    fun finnVedtaksperioderFor(vedtakId: Long): List<VedtaksperiodeMedBegrunnelser> =
        vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId)
}
