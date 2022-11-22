package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TrekkILøpendeUtbetalingService(
    @Autowired
    private val vedtakRepository: VedtakRepository
) {
    fun leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling: TrekkILøpendeUtbetaling) {
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(trekkILøpendeUtbetaling.behandlingId)
        val v = VedtaksperiodeMedBegrunnelser(type = Vedtaksperiodetype.TREKK_I_LØPENDE_UTBETALING, vedtak = vedtak)
        // vedtaksperiodeHentOgPersisterService.lagre(v)
    }

    fun hentTrekkILøpendeUtbetalinger() {
        TODO("Not yet implemented")
    }
}