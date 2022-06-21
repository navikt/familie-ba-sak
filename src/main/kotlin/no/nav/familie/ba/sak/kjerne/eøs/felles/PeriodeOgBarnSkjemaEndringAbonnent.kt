package no.nav.familie.ba.sak.kjerne.eøs.felles

interface PeriodeOgBarnSkjemaEndringAbonnent<S : PeriodeOgBarnSkjema<S>> {

    // Legger til abonnentIndeks for å kunne styre rekkefølgen abonnenter kalles.
    fun abonnentIndeks(): Int {
        return 0
    }

    fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<S>)
}
