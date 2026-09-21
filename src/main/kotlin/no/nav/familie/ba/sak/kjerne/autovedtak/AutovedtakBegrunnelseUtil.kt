package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.YearMonth

internal fun leggTilBegrunnelseIVedtaksperiode(
    vedtaksperiodeStartDato: YearMonth,
    standardbegrunnelse: Standardbegrunnelse,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) {
    val vedtaksperiode =
        vedtaksperioder.find {
            standardbegrunnelse.vedtakBegrunnelseType in it.type.tillatteBegrunnelsestyper &&
                it.fom?.toYearMonth() == vedtaksperiodeStartDato
        } ?: run {
            secureLogger.info(
                "Finner ikke aktuell periode å begrunne med $standardbegrunnelse ved autovedtak. " +
                    "Periode: $vedtaksperiodeStartDato. " +
                    "Perioder: ${vedtaksperioder.map { "Periode(type=${it.type}, fom=${it.fom}, tom=${it.tom})" }}",
            )
            throw Feil("Finner ikke aktuell periode å begrunne ved autovedtak. Se securelogger for detaljer.")
        }

    vedtaksperiode.settBegrunnelser(
        (
            vedtaksperiode.begrunnelser +
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiode,
                    standardbegrunnelse = standardbegrunnelse,
                )
        ).toList(),
    )
}
