package no.nav.familie.ba.sak.ekstern.pensjon

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import no.nav.familie.ba.sak.common.PERSONIDENT_IKKE_GYLDIG_FEILMELDING
import no.nav.familie.ba.sak.common.PERSONIDENT_REGEX
import java.time.LocalDate
import java.time.YearMonth

const val PENSJON_HENT_BARNETRYGD_PATH = "/api/ekstern/pensjon/hent-barnetrygd"

data class BarnetrygdTilPensjonRequest(
    @field:Pattern(regexp = PERSONIDENT_REGEX, message = PERSONIDENT_IKKE_GYLDIG_FEILMELDING)
    val ident: String,
    @Schema(implementation = String::class, example = "2020-12-01")
    val fraDato: LocalDate,
)

/*
 * Finnes barna til personen det spørres på i flere fagsaker vil det være flere elementer i listen
 * Ett element pr. fagsak barnet er knyttet til.
 * Kan være andre personer enn mor og far.
 */
data class BarnetrygdTilPensjonResponse(
    val fagsaker: List<BarnetrygdTilPensjon>,
)

data class BarnetrygdTilPensjon(
    val fagsakEiersIdent: String,
    val barnetrygdPerioder: List<BarnetrygdPeriode>,
)

data class BarnetrygdPeriode(
    val personIdent: String,
    val delingsprosentYtelse: YtelseProsent,
    val ytelseTypeEkstern: YtelseTypeEkstern?,
    val utbetaltPerMnd: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val sakstypeEkstern: SakstypeEkstern,
    val kildesystem: String = "BA",
    @Schema(description = "Videreføring av felt som fulgte med gammel løype. Settes kun når kildesystem=Infotrygd")
    val pensjonstrygdet: Boolean? = null,
    @Schema(description = "Brukes til å avgjøre om pensjonspoeng skal godskrives når utbetaltPerMnd=0. Settes kun når kildesystem=BA")
    val norgeErSekundærlandMedNullUtbetaling: Boolean? = false,
    @Schema(description = "True dersom søker (fagsakeier) har selvstendig rett i perioden, dvs. det er vurdert at annen forelder er omfattet av norsk lovgivning (EØS). Alltid true/false når kildesystem=BA, null når kildesystem=Infotrygd")
    val søkerHarSelvstendigRett: Boolean? = null,
) {
    // personIdent utelates bevisst slik at perioder kan logges uten å eksponere personidenter
    override fun toString(): String =
        "BarnetrygdPeriode(delingsprosentYtelse=$delingsprosentYtelse, ytelseTypeEkstern=$ytelseTypeEkstern, utbetaltPerMnd=$utbetaltPerMnd, " +
            "stønadFom=$stønadFom, stønadTom=$stønadTom, sakstypeEkstern=$sakstypeEkstern, kildesystem=$kildesystem, " +
            "pensjonstrygdet=$pensjonstrygdet, norgeErSekundærlandMedNullUtbetaling=$norgeErSekundærlandMedNullUtbetaling, " +
            "søkerHarSelvstendigRett=$søkerHarSelvstendigRett)"
}

enum class YtelseTypeEkstern {
    ORDINÆR_BARNETRYGD,
    UTVIDET_BARNETRYGD,
    SMÅBARNSTILLEGG,
    FINNMARKSTILLEGG,
    SVALBARDTILLEGG,
}

enum class YtelseProsent {
    FULL,
    DELT,
    USIKKER,
}

enum class SakstypeEkstern {
    NASJONAL,
    EØS,
}
