package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.YearMonth

fun lagKompetanse(
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    søkersAktivitet: SøkersAktivitet? = null,
    annenForeldersAktivitet: AnnenForeldersAktivitet? = null,
    annenForeldersAktivitetsland: String? = null,
    barnetsBostedsland: String? = null,
    kompetanseResultat: KompetanseResultat? = null,
) = Kompetanse(
    fom = fom,
    tom = tom,
    barnAktører = barnAktører,
    søkersAktivitet = søkersAktivitet,
    annenForeldersAktivitet = annenForeldersAktivitet,
    annenForeldersAktivitetsland = annenForeldersAktivitetsland,
    barnetsBostedsland = barnetsBostedsland,
    resultat = kompetanseResultat
)
