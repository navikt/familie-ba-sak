package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse

data class RestPutVedtaksperiodeMedFritekster(
    val fritekster: List<String> = emptyList(),
)

data class RestPutVedtaksperiodeMedStandardbegrunnelser(
    val standardbegrunnelser: List<Standardbegrunnelse>,
)

data class RestPutGenererFortsattInnvilgetVedtaksperioder(
    val skalGenererePerioderForFortsattInnvilget: Boolean,
    val behandlingId: Long
)
