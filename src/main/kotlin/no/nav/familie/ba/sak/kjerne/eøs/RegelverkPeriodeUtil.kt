package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkPeriode
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.VilkårResultatMåned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object RegelverkPeriodeUtil {
    fun lagVilkårResultatMåneder(vilkårsresultater: Collection<VilkårResultat>): List<VilkårResultatMåned> {
        val vilkårsResultatMåneder: List<VilkårResultatMåned> = vilkårsresultater
            .flatMap {
                if (it.periodeFom == null)
                    throw IllegalArgumentException("fra-og-med kan ikke være null")
                else if (it.periodeTom == null) {
                    mapÅpenPeriode(it, it.periodeFom!!, MAX_MÅNED)
                } else {
                    splittVilkårResultatTilMåneder(it)
                }
            }
        return vilkårsResultatMåneder
    }

    fun slåSammenRegelverkMåneder(regelverkMåneder: List<RegelverkMåned>): List<RegelverkPeriode> {

        return regelverkMåneder
            .distinct()
            .groupBy { it.vurderesEtter }
            .mapValues { (_, måneder) ->
                måneder.sortedBy { it.måned }
                    .fold<RegelverkMåned, List<RegelverkPeriode>>(emptyList()) { liste, neste ->
                        val siste = liste.lastOrNull()
                        if (siste?.tom == neste.måned.minusMonths(1) && siste?.vurderesEtter == neste.vurderesEtter) {
                            val nySiste = siste!!.copy(tom = neste.måned)
                            liste.subList(0, liste.size - 1) + listOf(nySiste)
                        } else {
                            liste + listOf(
                                RegelverkPeriode(
                                    fom = neste.måned,
                                    tom = neste.måned,
                                    vurderesEtter = neste.vurderesEtter
                                )
                            )
                        }
                    }.map {
                        if (it.tom == MAX_MÅNED) {
                            it.copy(tom = null)
                        } else {
                            it
                        }
                    }
            }.values.flatten()
    }

    private fun splittVilkårResultatTilMåneder(it: VilkårResultat): List<VilkårResultatMåned> {
        if (it.periodeFom == null || it.periodeTom == null) {
            return emptyList()
        }

        val månederImellom = ChronoUnit.MONTHS.between(
            it.periodeFom!!.withDayOfMonth(1),
            it.periodeTom!!.withDayOfMonth(1)
        )

        val fom = it.periodeFom!!.toYearMonth()
        val tom = fom.plusMonths(månederImellom)
        return (fom..tom).toList()
            .map { måned ->
                VilkårResultatMåned(it.vilkårType, it.resultat, måned, it.vurderesEtter)
            }
    }

    private fun mapÅpenPeriode(vilkårResultat: VilkårResultat, dato: LocalDate, åpenPeriode: YearMonth) = listOf(
        VilkårResultatMåned(
            vilkårResultat.vilkårType,
            resultat = vilkårResultat.resultat,
            måned = dato.toYearMonth(),
            vurderesEtter = vilkårResultat.vurderesEtter
        ),
        VilkårResultatMåned(
            vilkårResultat.vilkårType,
            resultat = vilkårResultat.resultat,
            måned = åpenPeriode,
            vurderesEtter = vilkårResultat.vurderesEtter
        )
    )
}
