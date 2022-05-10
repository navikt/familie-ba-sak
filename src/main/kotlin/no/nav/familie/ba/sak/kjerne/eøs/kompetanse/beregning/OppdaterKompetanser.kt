package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.bareSkjema
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.medBarnOgPeriodeSomErFellesMed

/**
 * Lager nye kompetanser der [oppdatering] overskriver skjemaet i [kompetanser] som helt eller delvis overlapper.
 * Hvis ingenting overlapper eller [oppdatering] ikke fører til endringer, så returneres [kompetanser]
 * @param[kompetanser]
 * @param[oppdatering]
 */
fun oppdaterKompetanserRekursivt(kompetanser: Collection<Kompetanse>, oppdatering: Kompetanse): Collection<Kompetanse> {
    // Finn første kompetanse som overlapper med oppdatering og som har avvik i skjemaet, og derfor må endres
    val førsteKompetanseSomMåOppdateres = kompetanser
        .filter { it.medBarnOgPeriodeSomErFellesMed(oppdatering) != null } // Må overlappe i periode og barn
        .filter { it.bareSkjema() != oppdatering.bareSkjema() } // Må være en endring av skjema
        .firstOrNull() ?: return kompetanser

    // Lag en oppdatering der barn og periode "ligger innenfor" kompetansen som skal oppdateres
    val tilpassetOppdatering = oppdatering.medBarnOgPeriodeSomErFellesMed(førsteKompetanseSomMåOppdateres)!!
    val kompetanseFratrukketOppdatering = førsteKompetanseSomMåOppdateres.trekkFra(tilpassetOppdatering)

    val oppdaterteKompetanser = kompetanser
        .minus(førsteKompetanseSomMåOppdateres)
        .plus(tilpassetOppdatering)
        .plus(kompetanseFratrukketOppdatering)
        .slåSammen()

    return oppdaterKompetanserRekursivt(oppdaterteKompetanser, oppdatering)
}
