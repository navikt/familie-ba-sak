package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class PutVedtaksperiodeMedFriteksterDto(
    val fritekster: List<String> = emptyList(),
)

data class PutVedtaksperiodeMedStandardbegrunnelserDto(
    val standardbegrunnelser: List<String>,
)

data class GenererVedtaksperioderForOverstyrtEndringstidspunktDto(
    val behandlingId: Long,
    val overstyrtEndringstidspunkt: LocalDate,
)

data class PutGenererFortsattInnvilgetVedtaksperioderDto(
    val skalGenererePerioderForFortsattInnvilget: Boolean,
    val behandlingId: Long,
)
