package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.kontrakter.felles.Fødselsnummer
import java.time.LocalDate

data class ManuellDødsfallDto(
    val dødsfallDato: LocalDate,
    val personIdent: String,
    val begrunnelse: String,
) {
    // Bruker init til å validere personidenten
    init {
        Fødselsnummer(personIdent)
    }
}
