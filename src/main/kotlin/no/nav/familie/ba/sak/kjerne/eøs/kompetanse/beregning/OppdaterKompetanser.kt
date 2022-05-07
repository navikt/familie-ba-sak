package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.bareSkjema
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.medBarnOgPeriodeSomOverlapperMed

/**
 * Lager nye kompetanser der [oppdatering] overskriver skjemaet i [kompetanser]
 * som helt eller delvis overlapper. Hvis ingenting overlapper, så returneres [kompetanser]
 * @param[kompetanser]
 * @param[oppdatering]
 */
fun oppdaterKompetanserRekursivt(kompetanser: Collection<Kompetanse>, oppdatering: Kompetanse): Collection<Kompetanse> {
    val kompetanseSomOppdateres = kompetanser
        .filter { it.medBarnOgPeriodeSomOverlapperMed(oppdatering) != null } // Må overlappe i periode og barn
        .filter { it.bareSkjema() != oppdatering.bareSkjema() } // Må være en endring av skjema
        .firstOrNull() ?: return kompetanser

    val endretKompetanse = oppdatering.medBarnOgPeriodeSomOverlapperMed(kompetanseSomOppdateres)!!
    val kompetanseFratrukketOppdatering = kompetanseSomOppdateres.trekkFra(endretKompetanse)

    val oppdaterteKompetanser = kompetanser
        .minus(kompetanseSomOppdateres)
        .plus(endretKompetanse)
        .plus(kompetanseFratrukketOppdatering)
        .slåSammen()

    return oppdaterKompetanserRekursivt(oppdaterteKompetanser, oppdatering)
}
