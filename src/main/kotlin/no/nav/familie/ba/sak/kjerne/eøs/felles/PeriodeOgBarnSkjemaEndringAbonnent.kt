package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId

interface PeriodeOgBarnSkjemaEndringAbonnent<S : PeriodeOgBarnSkjema<S>> {
    fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<S>)
}
