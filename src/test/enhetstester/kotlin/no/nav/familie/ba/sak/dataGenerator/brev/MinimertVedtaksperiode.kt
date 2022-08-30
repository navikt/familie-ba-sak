package no.nav.familie.ba.sak.dataGenerator.brev

import no.nav.familie.ba.sak.kjerne.brev.domene.BegrunnelseMedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedTriggere
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

fun lagMinimertVedtaksperiode(
    fom: LocalDate? = LocalDate.now().minusMonths(1),
    tom: LocalDate? = LocalDate.now(),
    type: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
    begrunnelser: List<BegrunnelseMedTriggere> = emptyList(),
    eøsBegrunnelser: List<EØSBegrunnelseMedTriggere> = emptyList(),
    fritekster: List<String> = emptyList(),
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj> = emptyList()
) = MinimertVedtaksperiode(
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    eøsBegrunnelser = eøsBegrunnelser,
    fritekster = fritekster,
    minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer
)
