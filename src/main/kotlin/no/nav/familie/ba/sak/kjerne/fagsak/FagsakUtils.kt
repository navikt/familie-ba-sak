package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.YearMonth

object FagsakUtils {
    fun fagsakBegrunnetMedBegrunnelse(
        vedtaksperiodeMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
        standardbegrunnelser: List<VedtakBegrunnelseSpesifikasjon>,
        måned: YearMonth
    ): Boolean {
        return vedtaksperiodeMedBegrunnelser.any {
            it.fom?.toYearMonth() == måned && it.begrunnelser.any { standardbegrunnelse -> standardbegrunnelse.vedtakBegrunnelseSpesifikasjon in standardbegrunnelser }
        }
    }
}
