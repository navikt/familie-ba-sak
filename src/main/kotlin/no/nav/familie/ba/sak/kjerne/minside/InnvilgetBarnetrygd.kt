package no.nav.familie.ba.sak.kjerne.minside

import java.time.YearMonth

data class InnvilgetBarnetrygd(
    val ordinær: Ordinær? = null,
    val utvidet: Utvidet? = null,
) {
    data class Ordinær(
        val startMåned: YearMonth,
    )

    data class Utvidet(
        val startMåned: YearMonth,
    )

    companion object {
        fun opprettIngenInnvilgetBarnetrygd(): InnvilgetBarnetrygd = InnvilgetBarnetrygd()
    }
}
