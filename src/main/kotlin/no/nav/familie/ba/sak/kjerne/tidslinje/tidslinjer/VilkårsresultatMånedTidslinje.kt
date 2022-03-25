package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.common.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.replaceLast
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

class VilkårsresultatMånedTidslinje(
    private val vilkårResultatTidslinje: VilkårResultatTidslinje,
) : Tidslinje<VilkårRegelverkResultat, Måned>() {

    override fun fraOgMed() = vilkårResultatTidslinje.fraOgMed().tilInneværendeMåned()

    override fun tilOgMed() = vilkårResultatTidslinje.tilOgMed().tilInneværendeMåned()

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat, Måned>> {
        return vilkårResultatTidslinje.perioder()
            .fold(emptyList()) { perioder, periode ->
                val sistePeriode = perioder.lastOrNull()
                val erBack2BackIMånedsskifte = if (sistePeriode != null) erBack2BackIMånedsskifte(
                    tilOgMed = sistePeriode.tilOgMed.tilLocalDate(),
                    fraOgMed = periode.fraOgMed.tilLocalDate()
                ) else false

                val oppdatertFraOgMed = periode.fraOgMed.tilInneværendeMåned().neste()
                val oppdatertTilOgMed = when (periode.innhold?.vilkår) {
                    Vilkår.UNDER_18_ÅR -> periode.tilOgMed.tilInneværendeMåned()
                        .forrige()
                    else -> periode.tilOgMed.tilInneværendeMåned()
                }

                when {
                    sistePeriode != null && erBack2BackIMånedsskifte ->
                        perioder.replaceLast(sistePeriode.copy(tilOgMed = sistePeriode.tilOgMed.neste())) + Periode(
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
