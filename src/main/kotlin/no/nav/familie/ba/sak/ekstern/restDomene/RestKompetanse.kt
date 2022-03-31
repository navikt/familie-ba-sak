package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.vurderStatus
import java.time.YearMonth

data class RestKompetanse(
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val søkersAktivitet: String? = null,
    val annenForeldersAktivitet: String? = null,
    val barnetsBostedsland: String? = null,
    val primærland: String? = null,
    val sekundærland: String? = null,
    val status: KompetanseStatus = KompetanseStatus.IKKE_UTFYLT
)

fun Kompetanse.tilRestKompetanse() = RestKompetanse(
    fom = this.fom,
    tom = this.tom,
    barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
    søkersAktivitet = this.søkersAktivitet,
    annenForeldersAktivitet = this.annenForeldersAktivitet,
    barnetsBostedsland = this.barnetsBostedsland,
    primærland = this.primærland,
    sekundærland = this.sekundærland,
    status = this.vurderStatus()
)
