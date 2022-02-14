package no.nav.familie.ba.sak.kjerne.kompetanse.domene

import java.time.YearMonth

data class Kompetanse(
    val id: Long = 0,
    val behandlingId: Long = 0,
    val fom: YearMonth,
    val tom: YearMonth,
    val barn: Set<Long>,
    val søkersAktivitet: String? = null,
    val annenForeldersAktivitet: String? = null,
    val barnetsBostedsland: String? = null,
    val primærland: String? = null,
    val sekundærland: String? = null,
)
