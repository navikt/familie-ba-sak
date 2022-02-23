package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårRegelverk
import java.time.LocalDate
import java.time.YearMonth

val MAX_MÅNED = LocalDate.MAX.toYearMonth()
val MIN_MÅNED = LocalDate.MIN.toYearMonth()

data class VilkårResultatMåned(
    val vilkårType: Vilkår,
    val resultat: Resultat?,
    val måned: YearMonth,
    val vurderesEtter: VilkårRegelverk?
)

fun Collection<VilkårResultatMåned>.ekspanderÅpnePerioder(): Collection<VilkårResultatMåned> {

    return this.groupBy { it.vilkårType }
        .mapValues { (_, resultater) ->
            resultater + resultater.ekspanderMaks(sisteFørMaks()?.måned)
        }.flatMap { (_, resultater) -> resultater }
}

private fun Collection<VilkårResultatMåned>.ekspanderMaks(
    oppTil: YearMonth?
): Collection<VilkårResultatMåned> {
    val sisteResultat = this.sisteFørMaks()
    return if (this.firstOrNull { it.måned == MAX_MÅNED } == null || oppTil == null || sisteResultat == null) {
        emptyList()
    } else {
        (sisteResultat.måned.plusMonths(1)..oppTil).map { sisteResultat.copy(måned = it) }
    }
}

private fun Collection<VilkårResultatMåned>.sisteFørMaks(): VilkårResultatMåned? =
    this.filter { it.måned != MAX_MÅNED && it.måned != MIN_MÅNED }
        .maxByOrNull { it.måned }
