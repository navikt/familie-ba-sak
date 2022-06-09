package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.util.SkjemaBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class KompetanseBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: BehandlingId = BehandlingId(1)
) : SkjemaBuilder<Kompetanse, KompetanseBuilder>(startMåned, behandlingId) {

    fun medKompetanse(k: String, vararg barn: Person, annenForeldersAktivitetsland: String? = null) = medSkjema(k, barn.toList()) {
        when (it) {
            '-' -> Kompetanse.NULL.copy(annenForeldersAktivitetsland = annenForeldersAktivitetsland)
            'S' -> Kompetanse.NULL.copy(resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND, annenForeldersAktivitetsland = annenForeldersAktivitetsland)
            'P' -> Kompetanse.NULL.copy(resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND, annenForeldersAktivitetsland = annenForeldersAktivitetsland)
            else -> null
        }
    }

    fun byggKompetanser(): Collection<Kompetanse> = bygg()
}
