package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate

data class PeriodeResultat(
    val aktør: Aktør,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val vilkårResultater: Set<PeriodeVilkår>
) {

    fun allePåkrevdeVilkårErOppfylt(personType: PersonType): Boolean {
        val alleVilkår = Vilkår.hentVilkårFor(personType)
        return vilkårResultater.map { it.vilkårType }.containsAll(alleVilkår) &&
            vilkårResultater.all { it.resultat == Resultat.OPPFYLT }
    }

    fun overlapper(annetPeriodeResultat: PeriodeResultat): Boolean {
        if (periodeFom == null && annetPeriodeResultat.periodeFom == null) {
            throw FunksjonellFeil(
                melding = "Enten søker eller barn må ha fom-dato på vilkårsresultatet",
                frontendFeilmelding = "Du må sette en fom-dato på minst et vilkår i vilkårsvurderingen"
            )
        }
        if (periodeTom == null && annetPeriodeResultat.periodeTom == null) {
            throw FunksjonellFeil(
                melding = "Enten søker eller barn må ha tom-dato på vilkårsresultatet",
                frontendFeilmelding = "Du må sette en tom-dato på minst et vilkår i vilkårsvurderingen"
            )
        }

        return (periodeFom == null || annetPeriodeResultat.periodeTom == null || periodeFom <= annetPeriodeResultat.periodeTom) &&
            (periodeTom == null || annetPeriodeResultat.periodeFom == null || periodeTom >= annetPeriodeResultat.periodeFom)
    }

    fun erDeltBostedSomSkalDeles() =
        vilkårResultater.firstOrNull { it.vilkårType == Vilkår.BOR_MED_SØKER }?.utdypendeVilkårsvurderinger?.contains(
            UtdypendeVilkårsvurdering.DELT_BOSTED
        ) ?: false
}

data class PeriodeVilkår(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    var begrunnelse: String,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?
)

private fun kombinerVerdier(
    lhs: LocalDateTimeline<List<VilkårResultat>>,
    rhs: LocalDateTimeline<VilkårResultat>
): LocalDateTimeline<List<VilkårResultat>> {
    return lhs.combine(
        rhs,
        { datoIntervall, sammenlagt, neste ->
            StandardCombinators.allValues(
                datoIntervall,
                sammenlagt,
                neste
            )
        },
        LocalDateTimeline.JoinStyle.CROSS_JOIN
    )
}

fun lagTidslinjeMedOverlappendePerioder(tidslinjer: List<LocalDateTimeline<VilkårResultat>>): LocalDateTimeline<List<VilkårResultat>> {
    if (tidslinjer.isEmpty()) return LocalDateTimeline(emptyList())
    val førsteSegment = tidslinjer.first().toSegments().first()
    val initiellSammenlagt =
        LocalDateTimeline(listOf(LocalDateSegment(førsteSegment.fom, førsteSegment.tom, listOf(førsteSegment.value))))
    val resterende = tidslinjer.drop(1)
    return resterende.fold(initiellSammenlagt) { sammenlagt, neste -> (kombinerVerdier(sammenlagt, neste)) }
}
