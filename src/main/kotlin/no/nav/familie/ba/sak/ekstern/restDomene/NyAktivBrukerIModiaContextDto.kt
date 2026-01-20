package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.kontrakter.felles.Fødselsnummer

data class NyAktivBrukerIModiaContextDto(
    val personIdent: String,
) {
    init {
        Fødselsnummer(personIdent)
    }
}
