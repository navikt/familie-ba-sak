package no.nav.familie.ba.sak.ekstern.pensjon

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.YearMonth

data class BarnetrygdTilPensjonRequest(
    val ident: String,
    @Schema(implementation = String::class, example = "2020-12-01")
    val fraDato: LocalDate,
)

/*
 * Finnes barna til personen det spørres på i flere fagsaker vil det være flere elementer i listen
 * Ett element pr. fagsak barnet er knyttet til.
 * Kan være andre personer enn mor og far.
 */
data class BarnetrygdTilPensjonResponse(val fagsaker: List<BarnetrygdTilPensjon>)

data class BarnetrygdTilPensjon(
    val fagsakEiersIdent: String,
    val barnetrygdPerioder: List<BarnetrygdPeriode>,
)

data class BarnetrygdPeriode(
    val personIdent: String,
    val delingsprosentYtelse: YtelseProsent,
    val ytelseTypeEkstern: YtelseTypeEkstern?,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val kildesystem: String = "BA"
)

enum class YtelseTypeEkstern {
    ORDINÆR_BARNETRYGD,
    UTVIDET_BARNETRYGD,
    SMÅBARNSTILLEGG,
}

enum class YtelseProsent {
    FULL,
    DELT,
    USIKKER
}