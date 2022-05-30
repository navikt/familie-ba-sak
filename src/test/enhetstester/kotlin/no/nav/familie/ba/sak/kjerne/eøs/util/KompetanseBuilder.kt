package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.util.SkjemaBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class KompetanseBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: Long = 1
) : SkjemaBuilder<Kompetanse, KompetanseBuilder>(startMåned, behandlingId) {

    fun medKompetanse(k: String, vararg barn: Person) = medSkjema(k, barn.toList()) {
        when (it) {
            '-' -> Kompetanse.NULL
            'S' -> Kompetanse.NULL.copy(resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND)
            'P' -> Kompetanse.NULL.copy(resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND)
            else -> null
        }
    }

    fun byggKompetanser(): Collection<Kompetanse> = bygg()
}
