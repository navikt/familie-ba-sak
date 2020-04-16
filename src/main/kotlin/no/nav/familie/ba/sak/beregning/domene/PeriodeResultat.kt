package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.fpsak.tidsserie.*
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

data class PeriodeResultat(
        val personIdent: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val vilkårResultater: Set<PeriodeVilkår>
) {

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

fun BehandlingResultat.personResultaterTilPeriodeResultater(): Set<PeriodeResultat> {
    return this.personResultater.flatMap { it.tilPeriodeResultater() }.toSet()
}

private fun kombinerVerdier(lhs: LocalDateTimeline<List<VilkårResultat>>,
                            rhs: LocalDateTimeline<VilkårResultat>): LocalDateTimeline<List<VilkårResultat>> {
    return lhs.combine(rhs,
                       { datoIntervall, sammenlagt, neste ->
                           StandardCombinators.allValues<VilkårResultat>(datoIntervall,
                                                                         sammenlagt,
                                                                         neste)
                       },
                       LocalDateTimeline.JoinStyle.CROSS_JOIN)
}

fun lagTidslinjeMedOverlappendePerioder(tidslinjer: List<LocalDateTimeline<VilkårResultat>>): LocalDateTimeline<List<VilkårResultat>> {
    val førsteSegment = tidslinjer.first().toSegments().first()
    val initiellSammenlagt =
            LocalDateTimeline(listOf(LocalDateSegment(førsteSegment.fom, førsteSegment.tom, listOf(førsteSegment.value))))
    val resterende = tidslinjer.drop(1)
    return resterende.fold(initiellSammenlagt) { sammenlagt, neste -> (kombinerVerdier(sammenlagt, neste)) }
}

fun PersonResultat.tilPeriodeResultater(): List<PeriodeResultat> {
    val tidslinjer = this.vilkårResultater.map { vilkårResultat ->
        LocalDateTimeline(listOf(LocalDateSegment(vilkårResultat.periodeFom?.withDayOfMonth(1),
                                                  vilkårResultat.periodeTom?.with(lastDayOfMonth()),
                                                  vilkårResultat)))
    }
    val kombinertTidslinje = lagTidslinjeMedOverlappendePerioder(tidslinjer)
    val periodeResultater = kombinertTidslinje.toSegments().map { segment ->
        PeriodeResultat(
                personIdent = this.personIdent,
                periodeFom = segment.fom,
                periodeTom = segment.tom,
                vilkårResultater = segment.value.map { PeriodeVilkår(it.vilkårType, it.resultat, it.begrunnelse) }.toSet()
        )
    }
    return periodeResultater
}