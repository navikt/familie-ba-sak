package no.nav.familie.ba.sak.kjerne.minside

import java.time.YearMonth

data class MinSideBarnetrygd(
    val ordinær: Ordinær? = null,
    val utvidet: Utvidet? = null,
) {
    data class Ordinær(
        val startmåned: YearMonth,
    )

    data class Utvidet(
        val startmåned: YearMonth,
    )
}
