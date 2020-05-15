package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.SakType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_BEGYNNELSE
import no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate

data class PeriodeResultat(
        val personIdent: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val vilkårResultater: Set<PeriodeVilkår>
) {

    fun allePåkrevdeVilkårErOppfylt(personType: PersonType, sakType: SakType): Boolean {
        val alleVilkår = Vilkår.hentVilkårFor(personType, sakType)
        return vilkårResultater.map { it.vilkårType }.containsAll(alleVilkår)
               && vilkårResultater.all { it.resultat == Resultat.JA }
    }

    fun allePåkrevdeVilkårVurdert(personType: PersonType, sakType: SakType): Boolean {
        val alleVilkår = Vilkår.hentVilkårFor(personType, sakType)
        return vilkårResultater.map { it.vilkårType }.containsAll(alleVilkår)
               && vilkårResultater.all { it.resultat != Resultat.KANSKJE && it.periodeFom != null }
    }

    fun overlapper(annetPeriodeResultat: PeriodeResultat): Boolean {
        if (periodeFom == null && annetPeriodeResultat.periodeFom == null) {
            error("Enten søker eller barn må ha fom-dato på vilkårsresultatet")
        }
        if (periodeTom == null && annetPeriodeResultat.periodeTom == null) {
            error("Enten søker eller barn må ha tom-dato på vilkårsresultatet")
        }

        return (periodeFom == null || annetPeriodeResultat.periodeTom == null || periodeFom <= annetPeriodeResultat.periodeTom)
               && (periodeTom == null || annetPeriodeResultat.periodeFom == null || periodeTom >= annetPeriodeResultat.periodeFom)
    }
}

data class PeriodeVilkår(
        val vilkårType: Vilkår,
        val resultat: Resultat,
        var begrunnelse: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?
)

fun BehandlingResultat.personResultaterTilPeriodeResultater(brukMåned: Boolean): Set<PeriodeResultat> {
    return this.personResultater.flatMap { it.tilPeriodeResultater(brukMåned) }.toSet()
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

fun PersonResultat.tilPeriodeResultater(brukMåned: Boolean): List<PeriodeResultat> {
    val tidslinjer = this.vilkårResultater.map { vilkårResultat ->
        LocalDateTimeline(listOf(LocalDateSegment(if (brukMåned) vilkårResultat.periodeFom?.withDayOfMonth(1) else vilkårResultat.periodeFom,
                                                  if (brukMåned) vilkårResultat.periodeTom?.sisteDagIMåned() else vilkårResultat.periodeTom,
                                                  vilkårResultat)))
    }
    val kombinertTidslinje = lagTidslinjeMedOverlappendePerioder(tidslinjer)
    return kombinertTidslinje.toSegments().map { segment ->
        PeriodeResultat(
                personIdent = personIdent,
                periodeFom = if (segment.fom == TIDENES_BEGYNNELSE) null else segment.fom,
                periodeTom = if (segment.tom == TIDENES_ENDE) null else segment.tom,
                vilkårResultater = segment.value.map {
                    PeriodeVilkår(
                            it.vilkårType,
                            it.resultat,
                            it.begrunnelse,
                            it.periodeFom?.withDayOfMonth(1),
                            it.periodeTom?.sisteDagIMåned())
                }.toSet()
        )
    }
}
