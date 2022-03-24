package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import java.time.YearMonth

data class Kompetanse(
    val behandlingId: Long = 0,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnAktørIder: Set<String>,
    val status: KompetanseStatus? = KompetanseStatus.IKKE_UTFYLT,
    val søkersAktivitet: String? = null,
    val annenForeldersAktivitet: String? = null,
    val barnetsBostedsland: String? = null,
    val primærland: String? = null,
    val sekundærland: String? = null,
) {
    var id: Long = 0
}

enum class KompetanseStatus {
    IKKE_UTFYLT,
    UFULLSTENDIG,
    OK
}

fun Kompetanse.blankUt() = this.copy(
    søkersAktivitet = null,
    annenForeldersAktivitet = null,
    barnetsBostedsland = null,
    primærland = null,
    sekundærland = null,
    status = KompetanseStatus.IKKE_UTFYLT
).also { it.id = this.id }
