package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.replaceLast
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

/**
 * Forskyver fom til neste måned som blir i praksis betyr virkningstidspunktet
 */
class VilkårsresultatMånedTidslinje(
    private val vilkårResultatTidslinje: VilkårResultatTidslinje,
) : Tidslinje<VilkårRegelverkResultat, Måned>() {

    override fun fraOgMed() = vilkårResultatTidslinje.fraOgMed().tilInneværendeMåned()

    override fun tilOgMed() = vilkårResultatTidslinje.tilOgMed().tilInneværendeMåned()

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat, Måned>> {
        return vilkårResultatTidslinje.perioder()
            .fold(emptyList()) { perioder, periode ->
                val oppdatertFraOgMed = periode.fraOgMed.tilInneværendeMåned().neste()
                val oppdatertTilOgMed =
                    if (periode.innhold?.vilkår != Vilkår.UNDER_18_ÅR && periode.innhold?.personType != PersonType.SØKER)
                        periode.tilOgMed.tilInneværendeMåned().neste()
                    else periode.tilOgMed.tilInneværendeMåned()

                val sistePeriode = perioder.lastOrNull()

                when {
                    sistePeriode != null && sistePeriode.tilOgMed == oppdatertFraOgMed ->
                        perioder.replaceLast(sistePeriode.copy(tilOgMed = sistePeriode.tilOgMed.forrige())) + Periode(
                            oppdatertFraOgMed,
                            oppdatertTilOgMed,
                            periode.innhold
                        )
                    else ->
                        perioder + Periode(oppdatertFraOgMed, oppdatertTilOgMed, periode.innhold)
                }
            }
    }
}
