package no.nav.familie.ba.sak.behandling

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
                           { dateInterval, lhs, rhs -> StandardCombinators.allValues<RestVilkårResultatTmp>(dateInterval, lhs, rhs) }, LocalDateTimeline.JoinStyle.CROSS_JOIN)

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
        val vilkårSegment = tidslinjer.first().toSegments().first()
        val initSammenlagtTidslinje = LocalDateTimeline(listOf(LocalDateSegment(vilkårSegment.fom, vilkårSegment.tom, listOf(vilkårSegment.value))))
        return tidslinjer.fold(initSammenlagtTidslinje) { lhs, rhs -> (kombinerTidslinjer(lhs, rhs)) }
    }
    private fun vilkårResultatTilTimeline(it: VilkårTmp): LocalDateTimeline<VilkårTmp> =
            LocalDateTimeline(listOf(LocalDateSegment(LocalDate.parse(it.periode.fom),
                                                      LocalDate.parse(it.periode.tom),
                                                      it)))
    private fun  kombinerTidslinjer(lhs: LocalDateTimeline<List<VilkårTmp>>, rhs: LocalDateTimeline<VilkårTmp>): LocalDateTimeline<List<VilkårTmp>> {
        return lhs.combine(rhs, { dateInterval, lhs, rhs -> StandardCombinators.allValues<VilkårTmp>(dateInterval, lhs, rhs) }, LocalDateTimeline.JoinStyle.CROSS_JOIN)
    }

    @Test
    fun `Kombinert tidslinje returnerer rette rette vilkårsresultater for tidsintervaller`() {
        val d1 = LocalDate.now().minusMonths(4);
        val d2 = LocalDate.now().minusMonths(2)
        val d3 = LocalDate.now()
        val d4 = LocalDate.now().plusMonths(1)

        val vilkårA = VilkårTmp("", "A", "", RestPeriode("", ""), Resultat.JA)
        val vilkårB = VilkårTmp("", "B", "", RestPeriode("", ""), Resultat.NEI)
        val t1: LocalDateTimeline<List<VilkårTmp>> = LocalDateTimeline(listOf(LocalDateSegment(d1, d3, listOf(vilkårA))))
        val t2: LocalDateTimeline<VilkårTmp> = LocalDateTimeline(listOf(LocalDateSegment(d2, d4, vilkårB)))
        val kombinert: LocalDateTimeline<List<VilkårTmp>> = t1.combine(t2, { dateInterval, lhs, rhs -> StandardCombinators.allValues<VilkårTmp>(dateInterval, lhs, rhs) }, LocalDateTimeline.JoinStyle.CROSS_JOIN)
        assert(t1.toSegments().size.equals(1))
        assert(t2.toSegments().size.equals(1))
        assert(kombinert.toSegments().size.equals(3))
        val verdier1 = kombinert.getSegment(LocalDateInterval(d1,d2))
        val verdier2 = kombinert.getSegment(LocalDateInterval(d2.plusDays(1),d3))
        val verdier3 = kombinert.getSegment(LocalDateInterval(d3.plusDays(1),d4))
        assert(verdier1.value.size.equals(1))
        assert(verdier2.value.size.equals(2))
        assert(verdier3.value.size.equals(1))

        assert(verdier1.value[0].vilkårType.equals("A"))
        assert(verdier2.value[0].vilkårType.equals("A"))
        assert(verdier2.value[1].vilkårType.equals("B"))
        assert(verdier3.value[0].vilkårType.equals("B"))
    }


}
