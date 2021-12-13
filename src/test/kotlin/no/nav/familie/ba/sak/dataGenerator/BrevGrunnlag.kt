package no.nav.familie.ba.sak.dataGenerator

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
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

fun lagBrevBegrunnelseGrunnlagMedPersoner(
    vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
    vedtakBegrunnelseType: VedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
    triggesAv: TriggesAv = lagTriggesAv(),
    personIdenter: List<String> = emptyList(),
): BrevBegrunnelseGrunnlagMedPersoner {
    return BrevBegrunnelseGrunnlagMedPersoner(
        vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        triggesAv = triggesAv,
        personIdenter = personIdenter
    )
}
