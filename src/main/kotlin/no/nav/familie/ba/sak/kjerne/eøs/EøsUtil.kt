package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.VilkårResultatMåned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate
import java.time.YearMonth

object EøsUtil {

    val nødvendigeVilkår = listOf(
        Vilkår.UNDER_18_ÅR,
        Vilkår.BOR_MED_SØKER,
        Vilkår.GIFT_PARTNERSKAP,
        Vilkår.LOVLIG_OPPHOLD,
        Vilkår.BOSATT_I_RIKET
    )

    val eøsVilkår = listOf(
        Vilkår.BOR_MED_SØKER,
        Vilkår.LOVLIG_OPPHOLD,
        Vilkår.BOSATT_I_RIKET
    )

    fun utledEøsPerioder(vilkårsresultater: List<VilkårResultat>) {
        utledMånederMedRegelverk(
            vilkårsresultater.flatMap {
                splittPeriode(it.periodeFom, it.periodeTom)
            }
        )
    }

    private fun splittPeriode(periodeFom: LocalDate?, periodeTom: LocalDate?): List<VilkårResultatMåned> {
        return emptyList()
    }

    fun utledEøsPerioderFraMåneder(vilkårsresultater: List<VilkårResultatMåned>) {
        utledMånederMedRegelverk(vilkårsresultater)
    }

    fun utledMånederMedRegelverk(vilkårsresultater: List<VilkårResultatMåned>): List<RegelverkMåned> {
        val regelverkMåneder = vilkårsresultater
            // Fyll opp med perioder slik at alle vilkårsperioder er like lange
            .ekspanderMaksOgMin()
            // Ta kun med oppfylte vilkår
            .filter { it.resultat == Resultat.OPPFYLT }
            // Ta bort vilkår som ikke er relevante
            .filter { nødvendigeVilkår.contains(it.vilkårType) }
            // Samle alle vilkårsresultater i samme måned
            .groupBy { it.måned }
            // ... deretter per vilkårstype
            .mapValues { (_, vilkårsresultat) -> vilkårsresultat.groupBy { it.vilkårType } }
            // Hvis ikke alle nødvendige vilkår finnes, så er det ikke en gyldig periode
            .filterValues { typeResultatMap -> typeResultatMap.keys.containsAll(nødvendigeVilkår) }
            // Finn ut om det er EØS-måned
            .mapValues { (_, vilkårResultatMap) ->
                val antallEøsVilkår = vilkårResultatMap
                    // Ta kun med EØS-vilkåtene
                    .filter { (vilkår, _) -> eøsVilkår.contains(vilkår) }
                    // Alle vilkår må ha blitt vurdert etter EØS-forordningen
                    .filter { (_, resultat) -> resultat.all { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN } }
                    .count()
                when (antallEøsVilkår) {
                    // Hvis vi står igjen med alle EØS-vilkårene, så er det en EØS-periode
                    eøsVilkår.size -> Regelverk.EØS_FORORDNINGEN
                    // Ingen EØS-vilkår betyr nasjonale regler
                    0 -> Regelverk.NASJONALE_REGLER
                    // Alt annet er en miks av regelverk. Det er feil
                    else -> null
                }
            }.map { (måned, regelverk) -> RegelverkMåned(måned, regelverk) }

        // Fjern EØS-perioder som mangler en måned med regelverk foran seg
        return regelverkMåneder
            .sortedBy { it.måned }
            .filterIndexed { index, regelverkMåned ->
                regelverkMåned.vurderesEtter != Regelverk.EØS_FORORDNINGEN ||
                    (index > 0 && harMånedMedRegelverkRettFør(regelverkMåned, regelverkMåneder[index - 1]))
            }
    }

    private fun Collection<VilkårResultatMåned>.ekspanderMaksOgMin(): Collection<VilkårResultatMåned> {

        return this.groupBy { it.vilkårType }
            .mapValues { (_, resultater) ->
                resultater +
                    ekspanderMaks(resultater, sisteFørMaks()?.måned) +
                    ekspanderMin(resultater, førsteEtterMin()?.måned)
            }.flatMap { (_, resultater) -> resultater }
    }
}

private fun harMånedMedRegelverkRettFør(regelverkMåned: RegelverkMåned, forrigeRegelverkMåned: RegelverkMåned) =
    forrigeRegelverkMåned.vurderesEtter != null &&
        (
            forrigeRegelverkMåned.måned == regelverkMåned.måned.minusMonths(1) ||
                regelverkMåned.måned == MAX_MÅNED
            )

private fun Collection<VilkårResultatMåned>.sisteFørMaks(): VilkårResultatMåned? =
    this.filter { it.måned != MAX_MÅNED && it.måned != MIN_MÅNED }
        .maxByOrNull { it.måned }

private fun Collection<VilkårResultatMåned>.førsteEtterMin(): VilkårResultatMåned? =
    this.filter { it.måned != MAX_MÅNED && it.måned != MIN_MÅNED }
        .minByOrNull { it.måned }

private fun ekspanderMaks(
    resultater: Collection<VilkårResultatMåned>,
    oppTil: YearMonth?
): Collection<VilkårResultatMåned> {
    val sisteResultat = resultater.sisteFørMaks()
    return if (resultater.firstOrNull { it.måned == MAX_MÅNED } == null || oppTil == null || sisteResultat == null) {
        emptyList()
    } else {
        (sisteResultat.måned..oppTil).map { sisteResultat.copy(måned = it) }
    }
}

private fun ekspanderMin(
    resultater: Collection<VilkårResultatMåned>,
    nedTil: YearMonth?
): Collection<VilkårResultatMåned> {
    val førsteResultat = resultater.førsteEtterMin()
    return if (resultater.firstOrNull { it.måned == MIN_MÅNED } == null || nedTil == null || førsteResultat == null) {
        emptyList()
    } else {
        (nedTil..førsteResultat.måned).map { førsteResultat.copy(måned = it) }
    }
}
