package no.nav.familie.ba.sak.ekstern.pensjon

import java.time.YearMonth

data class BarnetrygdTilPensjonRequest(val ident: String, val fom: YearMonth)

/*
 * Finnes barna til personen det spørres på i flere fagsaker vil det være flere elementer i listen
 * Ett element pr. fagsak barnet er knyttet til.
 * Kan være andre personer enn mor og far.
 */
data class BarnetrygdTilPensjonResponse(val list: List<BarnetrygdTilPensjon>)

data class BarnetrygdTilPensjon(
    val fagsakId: String,
    val fagsakEiersIdent: String,
    val barnetrygdPerioder: List<BarnetrygdPeriode>,
)

data class BarnetrygdPeriode(
    val personIdent: String,
    val delingsprosentYtelse: Int,
    val ytelseType: YtelseType?,
    val utbetaltPerMnd: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth
)

enum class YtelseType {
    ORDINÆR_BARNETRYGD,
    UTVIDET_BARNETRYGD,
    SMÅBARNSTILLEGG
}
