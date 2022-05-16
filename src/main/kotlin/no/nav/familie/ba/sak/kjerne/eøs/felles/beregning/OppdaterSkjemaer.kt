package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.bareSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBarnOgPeriodeSomOverlapperMed

/**
 * Lager nye kompetanser der [oppdatering] overskriver skjemaet i [kompetanser]
 * som helt eller delvis overlapper. Hvis ingenting overlapper, så returneres [kompetanser]
 * @param[kompetanser]
 * @param[oppdatering]
 */
fun <T : PeriodeOgBarnSkjema<T>> oppdaterSkjemaerRekursivt(skjemaer: Collection<T>, oppdatering: T): Collection<T> {
    val kompetanseSomOppdateres = skjemaer
        .filter { it.medBarnOgPeriodeSomOverlapperMed(oppdatering) != null } // Må overlappe i periode og barn
        .filter { it.bareSkjema() != oppdatering.bareSkjema() } // Må være en endring av skjema
        .firstOrNull() ?: return skjemaer

    val endretKompetanse = oppdatering.medBarnOgPeriodeSomOverlapperMed(kompetanseSomOppdateres)!!
    val kompetanseFratrukketOppdatering = kompetanseSomOppdateres.trekkFra(endretKompetanse)

    val oppdaterteKompetanser = skjemaer
        .minus(kompetanseSomOppdateres)
        .plus(endretKompetanse)
        .plus(kompetanseFratrukketOppdatering)
        .slåSammen()

    return oppdaterSkjemaerRekursivt(oppdaterteKompetanser, oppdatering)
}
