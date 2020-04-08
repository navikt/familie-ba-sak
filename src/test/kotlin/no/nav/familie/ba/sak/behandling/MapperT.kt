package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.RestPersonInfo
import no.nav.familie.ba.sak.behandling.restDomene.RestPeriode
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultatTmp
import no.nav.familie.ba.sak.behandling.restDomene.VilkårTmp
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class MapperT {






    @Test
    fun `test combiner`() {
        val t1: LocalDateTimeline<List<String>> = LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(4), LocalDate.now(), listOf("HEI"))))

        val t2: LocalDateTimeline<String> = LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(1), "HALLO")))

        val kombinert = t1.combine<String, List<String>>(t2,
                                                         { dateInterval, lhs, rhs ->
                                                             StandardCombinators.allValues<String>(dateInterval,
                                                                                           lhs,
                                                                                           rhs)
                                                         }, LocalDateTimeline.JoinStyle.CROSS_JOIN)

    }










    @Test
    fun `test mapping`() {
        val rvr = RestVilkårResultatTmp("", "", RestPeriode("", ""), Resultat.NEI)
        val t1: LocalDateTimeline<List<RestVilkårResultatTmp>> =
                LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(4), LocalDate.now(), listOf(rvr))))
        val t2: LocalDateTimeline<RestVilkårResultatTmp> = LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(2),
                                                                                                     LocalDate.now().plusMonths(1),
                                                                                                     rvr)))
        val kombinert: LocalDateTimeline<List<RestVilkårResultatTmp>> =
                t1.combine(t2,
                           { dateInterval, lhs, rhs -> StandardCombinators.allValues(dateInterval, lhs, rhs) }, LocalDateTimeline.JoinStyle.CROSS_JOIN)

    }


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

}
