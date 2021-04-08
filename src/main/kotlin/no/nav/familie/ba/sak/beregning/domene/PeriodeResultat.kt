package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_BEGYNNELSE
import no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate

data class PeriodeResultat(
        val personIdent: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val vilkårResultater: Set<PeriodeVilkår>
) {

    fun allePåkrevdeVilkårErOppfylt(personType: PersonType): Boolean {
        val alleVilkår = Vilkår.hentVilkårFor(personType)
        return vilkårResultater.map { it.vilkårType }.containsAll(alleVilkår)
               && vilkårResultater.all { it.resultat == Resultat.OPPFYLT }
    }

    fun allePåkrevdeVilkårVurdert(personType: PersonType): Boolean {
        val alleVilkår = Vilkår.hentVilkårFor(personType)
        return vilkårResultater.map { it.vilkårType }.containsAll(alleVilkår)
               && vilkårResultater.all { it.resultat != Resultat.IKKE_VURDERT }
    }

    fun overlapper(annetPeriodeResultat: PeriodeResultat): Boolean {
        if (periodeFom == null && annetPeriodeResultat.periodeFom == null) {
            throw FunksjonellFeil(melding = "Enten søker eller barn må ha fom-dato på vilkårsresultatet",
                                  frontendFeilmelding = "Du må sette en fom-dato på minst et vilkår i vilkårsvurderingen")
        }
        if (periodeTom == null && annetPeriodeResultat.periodeTom == null) {
            throw FunksjonellFeil(melding = "Enten søker eller barn må ha tom-dato på vilkårsresultatet",
                                  frontendFeilmelding = "Du må sette en tom-dato på minst et vilkår i vilkårsvurderingen")
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

fun Vilkårsvurdering.personResultaterTilPeriodeResultater(brukMåned: Boolean): Set<PeriodeResultat> {
    return this.personResultater.flatMap { it.tilPeriodeResultater(brukMåned) }.toSet()
}

private fun kombinerVerdier(lhs: LocalDateTimeline<List<VilkårResultat>>,
                            rhs: LocalDateTimeline<VilkårResultat>): LocalDateTimeline<List<VilkårResultat>> {
    return lhs.combine(rhs,
                       { datoIntervall, sammenlagt, neste ->
                           StandardCombinators.allValues(datoIntervall,
                                                         sammenlagt,
                                                         neste)
                       },
                       LocalDateTimeline.JoinStyle.CROSS_JOIN)
}

fun lagTidslinjeMedOverlappendePerioder(tidslinjer: List<LocalDateTimeline<VilkårResultat>>): LocalDateTimeline<List<VilkårResultat>> {
    if (tidslinjer.isEmpty()) return LocalDateTimeline(emptyList())
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
                            if (brukMåned) it.periodeFom?.withDayOfMonth(1) else it.periodeFom,
                            if (brukMåned) it.periodeTom?.sisteDagIMåned() else it.periodeTom)
                }.toSet()
        )
    }
}
