package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.kontrakter.felles.Fødselsnummer

data class RestHentFagsakerForPerson(
    val personIdent: String,
    val fagsakTyper: List<FagsakType> = FagsakType.entries.toList(),
) {
    // Bruker init til å validere personidenten
    init {
        Fødselsnummer(personIdent)
    }
}
