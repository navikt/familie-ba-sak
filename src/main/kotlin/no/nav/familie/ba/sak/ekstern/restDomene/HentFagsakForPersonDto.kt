package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.kontrakter.felles.Fødselsnummer

data class HentFagsakForPersonDto(
    val personIdent: String,
    val fagsakType: FagsakType = FagsakType.NORMAL,
) {
    // Bruker init til å validere personidenten
    init {
        Fødselsnummer(personIdent)
    }
}
