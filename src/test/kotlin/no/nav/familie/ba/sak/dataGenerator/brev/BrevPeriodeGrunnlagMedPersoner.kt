package no.nav.familie.ba.sak.dataGenerator.brev

import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

fun lagBrevPeriodeGrunnlagMedPersoner(
    fom: LocalDate? = LocalDate.now().minusMonths(1),
    tom: LocalDate? = LocalDate.now(),
    type: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
    begrunnelser: List<BrevBegrunnelseGrunnlagMedPersoner> = emptyList(),
    fritekster: List<String> = emptyList(),
    minimertUtbetalingsperiodeDetalj: List<MinimertUtbetalingsperiodeDetalj> = emptyList(),
    erFørsteVedtaksperiodePåFagsak: Boolean = false
): BrevPeriodeGrunnlagMedPersoner {
    return BrevPeriodeGrunnlagMedPersoner(
        fom = fom,
        tom = tom,
        type = type,
        begrunnelser = begrunnelser,
        fritekster = fritekster,
        minimerteUtbetalingsperiodeDetaljer = minimertUtbetalingsperiodeDetalj,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
    )
}