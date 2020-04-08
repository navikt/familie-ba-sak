package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import java.time.LocalDateTime


data class RestBehandling(val aktiv: Boolean,
                          val behandlingId: Long,
                          val type: BehandlingType,
                          val status: BehandlingStatus,
                          val steg: StegType,
                          val kategori: BehandlingKategori,
                          val personer: List<RestPerson>,
                          val opprettetTidspunkt: LocalDateTime,
                          val underkategori: BehandlingUnderkategori,
                          val periodeResultater: List<RestPeriodeResultat>,
                          //val personResultater: List<RestPersonResultat>,
                          val vedtakForBehandling: List<RestVedtak?>,
                          val begrunnelse: String)

data class RestPeriodeResultat(
        val personIdent: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val vilkårResultater: List<RestVilkårResultat>?
)

fun PeriodeResultat.tilRestPeriodeResultat() = RestPeriodeResultat(personIdent = this.personIdent,
                                                                   periodeFom = this.periodeFom,
                                                                   periodeTom = this.periodeTom,
                                                                   vilkårResultater = this.vilkårResultater.map { resultat ->
                                                                       RestVilkårResultat(resultat = resultat.resultat,
                                                                                          vilkårType = resultat.vilkårType)
                                                                   })

data class RestVilkårResultat(
        val vilkårType: Vilkår,
        val resultat: Resultat
)

data class RestPersonResultat(
        val personIdent: String, // Flytt et nivå ned for å bevare i reducer?
        val vilkårResultater: List<RestVilkårResultatTmp> //RENAME TIL VILKÅRRESULTATER
)

data class RestVilkårResultatTmp(
        val vilkårType: String,
        val begrunnelse: String,
        val periode: RestPeriode,
        val resultat: Resultat
)

data class VilkårTmp(
        val personIdent: String,
        val vilkårType: String,
        val begrunnelse: String,
        val periode: RestPeriode,
        val resultat: Resultat
)

data class RestPeriode(
        val fom: String?,
        val tom: String?
)

//fun BehandlingResultat.mapTilRest() = RestPersonResultat()

//fun RestPersonResultat.mapTilIntern() = BehandlingResultat()

/*

fun maptilfptidslinje(restPersonResultater: List<RestPersonResultat>): LocalDateTimeline<List<VilkårTmp>> {
    val flattMedPerson: List<VilkårTmp> = restPersonResultater.flatMap { personResultat ->
        personResultat.vilkårResultater.map {
            VilkårTmp(personIdent = personResultat.personIdent,
                      vilkårType = it.vilkårType,
                      begrunnelse = it.begrunnelse,
                      periode = it.periode,
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
        LocalDateTimeline(listOf(LocalDateSegment(LocalDate.parse(it.periode.fom),
                                                  LocalDate.parse(it.periode.tom),
                                                  it)))
private fun  kombinerTidslinjer(lhs: LocalDateTimeline<List<VilkårTmp>>, rhs: LocalDateTimeline<VilkårTmp>): LocalDateTimeline<List<VilkårTmp>> {
    return lhs.combine(rhs, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
}

*/

/*
TIL FORMAT:

class PeriodeResultat(
        //private val id: Long = 0,
        //var behandlingResultat: BehandlingResultat,
        val personIdent: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        var vilkårResultater: Set<VilkårResultat> = setOf())
class VilkårResultat(
        //val id: Long = 0,
        //var periodeResultat: PeriodeResultat,
        val vilkårType: Vilkår,
        val resultat: Resultat,
        var begrunnelse: String
)
*/

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