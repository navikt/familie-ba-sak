package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerDefault
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

data class VilkårRegelverkResultat(
    val vilkår: Vilkår,
    val regelverk: Regelverk?,
    val resultat: Resultat?
)

class VilkårResultatTidslinje(
    private val vilkårsresultater: List<VilkårResultat>,
    private val praktiskTidligsteDato: LocalDate,
    private val praktiskSenesteDato: LocalDate
) : Tidslinje<VilkårRegelverkResultat>() {

    override fun fraOgMed() = vilkårsresultater.minOf {
        it.periodeFom.tilTidspunktEllerDefault { praktiskTidligsteDato }.neste().tilInneværendeMåned()
    }

    override fun tilOgMed() = vilkårsresultater.maxOf {
        it.periodeTom.tilTidspunktEllerDefault { praktiskSenesteDato }.tilInneværendeMåned()
    }

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat>> {
        return vilkårsresultater.map { it.tilPeriode(praktiskTidligsteDato, praktiskSenesteDato) }
    }
}

fun VilkårResultat.tilPeriode(
    praktiskTidligsteDato: LocalDate,
    praktiskSenesteDato: LocalDate
): Periode<VilkårRegelverkResultat> {
    // Forskyv fom til neste måned som blir virkningstidspunktet
    val fom = periodeFom.tilTidspunktEllerDefault { praktiskTidligsteDato }.neste()
    val tom = periodeTom.tilTidspunktEllerDefault { praktiskSenesteDato }
    return Periode(fom, tom, VilkårRegelverkResultat(vilkårType, vurderesEtter, resultat))
}
