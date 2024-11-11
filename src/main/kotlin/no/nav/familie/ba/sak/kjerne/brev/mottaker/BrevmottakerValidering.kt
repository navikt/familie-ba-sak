package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker

object BrevmottakerValidering {
    fun erBrevmottakerGyldig(brevmottaker: ManuellBrevmottaker): Boolean {
        if (brevmottaker.landkode == "NO") {
            return brevmottaker.navn.isNotEmpty() &&
                brevmottaker.adresselinje1.isNotEmpty() &&
                brevmottaker.postnummer.isNotEmpty() &&
                brevmottaker.poststed.isNotEmpty()
        } else {
            // Utenlandske manuelle brevmottakere skal ha postnummer og poststed satt i adresselinjene
            return brevmottaker.navn.isNotEmpty() &&
                brevmottaker.adresselinje1.isNotEmpty() &&
                brevmottaker.postnummer.isEmpty() &&
                brevmottaker.poststed.isEmpty()
        }
    }

    fun erBrevmottakereGyldige(brevmottakere: List<ManuellBrevmottaker>): Boolean =
        brevmottakere.all {
            erBrevmottakerGyldig(it)
        }
}
