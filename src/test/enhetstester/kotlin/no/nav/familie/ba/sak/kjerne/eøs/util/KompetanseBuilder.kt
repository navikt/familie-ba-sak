package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.util.SkjemaBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt

class KompetanseBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: BehandlingId = BehandlingId(1)
) : SkjemaBuilder<Kompetanse, KompetanseBuilder>(startMåned, behandlingId) {

    fun medKompetanse(k: String, vararg barn: Person, annenForeldersAktivitetsland: String? = null) =
        medSkjema(k, barn.toList()) {
            when (it) {
                '-' -> Kompetanse.NULL.copy(annenForeldersAktivitetsland = annenForeldersAktivitetsland)
                'S' -> Kompetanse.NULL.copy(
                    resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    annenForeldersAktivitetsland = annenForeldersAktivitetsland
                ).fyllUt()

                'P' -> Kompetanse.NULL.copy(
                    resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                    annenForeldersAktivitetsland = annenForeldersAktivitetsland
                ).fyllUt()

                else -> null
            }
        }

    fun byggKompetanser(): Collection<Kompetanse> = bygg()
}

fun Kompetanse.fyllUt() = this.copy(
    resultat = resultat ?: KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
    annenForeldersAktivitetsland = annenForeldersAktivitetsland ?: "DK",
    barnetsBostedsland = barnetsBostedsland ?: "NO",
    søkersAktivitet = søkersAktivitet ?: SøkersAktivitet.ARBEIDER,
    annenForeldersAktivitet = annenForeldersAktivitet ?: AnnenForeldersAktivitet.I_ARBEID,
    søkersAktivitetsland = søkersAktivitetsland ?: "SE"
)
