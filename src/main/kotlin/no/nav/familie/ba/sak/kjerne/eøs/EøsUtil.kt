package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.RegelverkPeriodeUtil.lagVilkårResultatMåneder
import no.nav.familie.ba.sak.kjerne.eøs.RegelverkPeriodeUtil.slåSammenRegelverkMåneder
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkPeriode
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.VilkårResultatMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.ekspanderÅpnePerioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårRegelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

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

    fun utledEøsPerioder(vilkårsresultater: Collection<VilkårResultat>) =
        utledRegelverkPerioder(vilkårsresultater)
            .filter { it.vurderesEtter == VilkårRegelverk.EØS_FORORDNINGEN }
            .map { NullableMånedPeriode(fom = it.fom, tom = it.tom) }

    fun utledRegelverkPerioder(vilkårsresultater: Collection<VilkårResultat>): List<RegelverkPeriode> {
        val vilkårResultatMåneder = lagVilkårResultatMåneder(vilkårsresultater)
        val regelverkMåneder = utledMånederMedRegelverk(vilkårResultatMåneder)
        return slåSammenRegelverkMåneder(regelverkMåneder)
    }

    fun utledMånederMedRegelverk(vilkårsresultatMåneder: Collection<VilkårResultatMåned>): List<RegelverkMåned> {
        val vilkårRegelverkMåneder = vilkårsresultatMåneder
            // Fyll opp med perioder slik at alle vilkårsperioder som sammenliknes er like lange, om mulig
            .ekspanderÅpnePerioder()
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
                    .filter { (_, resultat) -> resultat.all { it.vurderesEtter == VilkårRegelverk.EØS_FORORDNINGEN } }
                    .count()
                when (antallEøsVilkår) {
                    // Hvis vi står igjen med alle EØS-vilkårene, så er det en EØS-periode
                    eøsVilkår.size -> VilkårRegelverk.EØS_FORORDNINGEN
                    // Ingen EØS-vilkår betyr nasjonale regler
                    0 -> VilkårRegelverk.NASJONALE_REGLER
                    // Alt annet er en miks av regelverk. Det er feil
                    else -> null
                }
            }.map { (måned, regelverk) -> RegelverkMåned(måned, regelverk) }

        // Fjern EØS-perioder som mangler en måned med regelverk foran seg
        return vilkårRegelverkMåneder
            .sortedBy { it.måned }
            .filterIndexed { index, regelverkMåned ->
                regelverkMåned.vurderesEtter != VilkårRegelverk.EØS_FORORDNINGEN ||
                    (index > 0 && harMånedMedRegelverkRettFør(regelverkMåned, vilkårRegelverkMåneder[index - 1]))
            }
    }
}

private fun harMånedMedRegelverkRettFør(regelverkMåned: RegelverkMåned, forrigeRegelverkMåned: RegelverkMåned) =
    forrigeRegelverkMåned.vurderesEtter != null &&
        (
            forrigeRegelverkMåned.måned == regelverkMåned.måned.minusMonths(1) ||
                regelverkMåned.måned == MAX_MÅNED
            )
