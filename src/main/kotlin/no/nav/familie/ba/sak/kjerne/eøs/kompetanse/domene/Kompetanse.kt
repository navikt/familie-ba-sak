package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.YearMonth

data class Kompetanse(
    val id: Long = 0,
    val behandlingId: Long = 0,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barn: Set<String>,
    val skjema: KompetanseSkjema? = null,
    @JsonUnwrapped
    val status: KompetanseStatus? = null
)

data class KompetanseSkjema(
    val søkersAktivitet: String? = null,
    val annenForeldersAktivitet: String? = null,
    val barnetsBostedsland: String? = null,
    val primærland: String? = null,
    val sekundærland: String? = null,
)

enum class KompetanseStatus {
    IKKE_UTFYLT,
    UFULLSTENDIG,
    OK
}

fun Kompetanse.blankUt() = this.copy(skjema = null, status = KompetanseStatus.IKKE_UTFYLT)
