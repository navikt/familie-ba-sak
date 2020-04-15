package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
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

/*
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
public fun <reified T : Any> LocalDateTimeline<*>.medType(): Boolean =
        T::class.java.isAssignableFrom(this::class.java.componentType)
private fun  kombinerTidslinjerReducer(lhs: LocalDateTimeline<*>, rhs: LocalDateTimeline<VilkårTmp>): LocalDateTimeline<List<VilkårTmp>> {
    //https://stackoverflow.com/questions/51136866/how-can-i-check-for-array-type-not-generic-type-in-kotlin
    //vurder å legge inn pr til tidslinjer for en istimelineof-metode ala isArrayOf: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/is-array-of.html
    return when {
        lhs.medType<VilkårTmp>() -> {
            lhs as LocalDateTimeline<VilkårTmp>
            val vilkårSegment = lhs.toSegments().first()
            val initSammenlagtTidslinje = LocalDateTimeline(listOf(LocalDateSegment(vilkårSegment.fom, vilkårSegment.tom, listOf(vilkårSegment.value))))
            initSammenlagtTidslinje.combine(rhs, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
        }
        lhs.medType<List<VilkårTmp>>() -> {
            lhs as LocalDateTimeline<List<VilkårTmp>>
            lhs.combine(rhs, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
        }
        else -> {
            throw IllegalArgumentException("prøverå reduce med type som ikke er gyldig")
        }
    }
}

val rvr = RestVilkårResultatTmp("", "", RestPeriode("", ""), Resultat.NEI)
val t1: LocalDateTimeline<List<RestVilkårResultatTmp>> =
        LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(4), LocalDate.now(), listOf(rvr))))
val t2: LocalDateTimeline<RestVilkårResultatTmp> = LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(2),
                                                                                          LocalDate.now().plusMonths(1),
                                                                                          rvr)))
val kombinert: LocalDateTimeline<List<RestVilkårResultatTmp>> =
        t1.combine(t2, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
*/

data class VilkårTmp(
        val personIdent: String,
        val vilkårType: Vilkår, //TODO: String?
        val resultat: Resultat,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val begrunnelse: String
)


fun maptilfptidslinje(restPersonResultater: List<RestPersonResultat>): LocalDateTimeline<List<VilkårTmp>> {
    val flattMedPerson: List<VilkårTmp> = restPersonResultater.flatMap { personResultat ->
        personResultat.vilkårResultater!!.map { //TODO: Fix
            VilkårTmp(personIdent = personResultat.personIdent,
                      vilkårType = it.vilkårType,
                      begrunnelse = it.begrunnelse,
                      periodeFom = it.periodeFom,
                      periodeTom = it.periodeFom,
                      resultat = it.resultat)
        }
    }
    val tidslinjer: List<LocalDateTimeline<VilkårTmp>> = flattMedPerson.map { vilkårTmp -> vilkårResultatTilTimeline(vilkårTmp) }
    //val samletTidslinje: LocalDateTimeline<List<VilkårTmp>> = tidslinjer.reduce(::kombinerTidslinjerREDUCER)  }
    //val samletTidslinje: LocalDateTimeline<List<VilkårTmp>> = tidslinjer.reduce { lhs, rhs -> (kombinerTidslinjerREDUCER(lhs, rhs))  }
    val vilkårSegment = tidslinjer.first().toSegments().first()
    val initSammenlagtTidslinje = LocalDateTimeline(listOf(LocalDateSegment(vilkårSegment.fom, vilkårSegment.tom, listOf(vilkårSegment.value))))
    return tidslinjer.fold(initSammenlagtTidslinje) { lhs, rhs -> (kombinerTidslinjer(lhs, rhs)) }
}

private fun vilkårResultatTilTimeline(it: VilkårTmp): LocalDateTimeline<VilkårTmp> =
        LocalDateTimeline(listOf(LocalDateSegment(it.periodeFom,
                                                  it.periodeTom,
                                                  it)))
private fun  kombinerTidslinjer(lhs: LocalDateTimeline<List<VilkårTmp>>, rhs: LocalDateTimeline<VilkårTmp>): LocalDateTimeline<List<VilkårTmp>> {
    return lhs.combine(rhs, { dateInterval, lhs, rhs -> StandardCombinators.allValues<VilkårTmp>(dateInterval, lhs, rhs) }, LocalDateTimeline.JoinStyle.CROSS_JOIN)
}

