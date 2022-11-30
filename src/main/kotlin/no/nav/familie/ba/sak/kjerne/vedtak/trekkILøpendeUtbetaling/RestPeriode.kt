package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import java.time.YearMonth

data class RestPeriode(
    val fom: YearMonth,
    val tom: YearMonth?
)
