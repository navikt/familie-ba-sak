package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker

object BrevmottakerValidering {
    fun erBrevmottakereGyldige(brevmottakere: List<ManuellBrevmottaker>): Boolean =
        brevmottakere.all {
            it.harGyldigAdresse()
        }
}
