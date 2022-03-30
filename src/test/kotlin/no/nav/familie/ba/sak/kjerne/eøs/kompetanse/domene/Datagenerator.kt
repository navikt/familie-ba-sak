package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.YearMonth

fun lagKompetanse(
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    søkersAktivitet: String? = null,
    annenForeldersAktivitet: String? = null,
    barnetsBostedsland: String? = null,
    primærland: String? = null,
    sekundærland: String? = null,
) = Kompetanse(
    fom = fom,
    tom = tom,
    barnAktører = barnAktører,
    søkersAktivitet = søkersAktivitet,
    annenForeldersAktivitet = annenForeldersAktivitet,
    barnetsBostedsland = barnetsBostedsland,
    primærland = primærland,
    sekundærland = sekundærland
)
