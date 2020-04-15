package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate

data class PeriodeResultat (
        val personIdent: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val vilkårResultater: Set<PeriodeVilkår>
){
    fun hentSamletResultat(): BehandlingResultatType {
        return when {
            vilkårResultater.all { it.resultat == Resultat.JA } -> {
                BehandlingResultatType.INNVILGET
            }
            else -> {
                BehandlingResultatType.AVSLÅTT
            }
        }
    }
}

data class PeriodeVilkår(
        val vilkårType: Vilkår,
        val resultat: Resultat,
        var begrunnelse: String)

fun BehandlingResultat.tilPeriodeResultater() : Set<PeriodeResultat> {
    return this.personResultater.map { personResultatTilPeriodeResultater(it) }.flatten().toSet()
}

private fun kombinerVerdier(lhs: LocalDateTimeline<List<VilkårResultat>>,
                            rhs: LocalDateTimeline<VilkårResultat>): LocalDateTimeline<List<VilkårResultat>> {
    return lhs.combine(rhs,
                       { dateInterval, left, right ->
                           StandardCombinators.allValues<VilkårResultat>(dateInterval,
                                                                         left,
                                                                         right)
                       },
                       LocalDateTimeline.JoinStyle.CROSS_JOIN)
}

fun foldTidslinjer(tidslinjer: List<LocalDateTimeline<VilkårResultat>>): LocalDateTimeline<List<VilkårResultat>> {
    val førsteSegment = tidslinjer.first().toSegments().first()
    val initiellSammenlagt =
            LocalDateTimeline(listOf(LocalDateSegment(førsteSegment.fom, førsteSegment.tom, listOf(førsteSegment.value))))
    val resterende = tidslinjer.drop(1)
    return resterende.fold(initiellSammenlagt) { sammenlagt, neste -> (kombinerVerdier(sammenlagt, neste)) }
}

fun personResultatTilPeriodeResultater(personResultat: PersonResultat): List<PeriodeResultat> {
    val tidslinjer = personResultat.vilkårResultater.map { vilkårResultat ->
        LocalDateTimeline(listOf(LocalDateSegment(vilkårResultat.periodeFom,
                                                  vilkårResultat.periodeTom,
                                                  vilkårResultat)))
    }
    val kombinertTidslinje = foldTidslinjer(tidslinjer)
    val periodeResultater = kombinertTidslinje.toSegments().map { segment ->
        PeriodeResultat(
                personIdent = personResultat.personIdent,
                periodeFom = segment.fom,
                periodeTom = segment.tom,
                vilkårResultater = segment.value.map { PeriodeVilkår(it.vilkårType, it.resultat, it.begrunnelse) }.toSet()
        )
    }
    return periodeResultater
}