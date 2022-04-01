package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import java.time.LocalDate

data class RestPutVedtaksperiodeMedFritekster(
    val fritekster: List<String> = emptyList(),
)

data class RestPutVedtaksperiodeMedStandardbegrunnelser(
    val standardbegrunnelser: List<Standardbegrunnelse>,
)

data class RestGenererVedtaksperioderForFørsteEndringstidspunkt(
    val behandlingId: Long,
    val førsteEndringstidspunkt: LocalDate
)

data class RestPutGenererFortsattInnvilgetVedtaksperioder(
    val skalGenererePerioderForFortsattInnvilget: Boolean,
    val behandlingId: Long
)
