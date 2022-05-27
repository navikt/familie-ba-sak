package no.nav.familie.ba.sak.dataGenerator.vedtak

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.NasjonalPeriodebegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun lagVedtaksbegrunnelse(
    standardbegrunnelse: Standardbegrunnelse =
        Standardbegrunnelse.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk()
) = NasjonalPeriodebegrunnelse(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    begrunnelse = standardbegrunnelse,
)
