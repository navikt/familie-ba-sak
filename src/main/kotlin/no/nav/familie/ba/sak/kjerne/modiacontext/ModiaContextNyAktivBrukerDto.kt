package no.nav.familie.ba.sak.kjerne.modiacontext

import no.nav.familie.kontrakter.felles.Fødselsnummer

data class ModiaContextNyAktivBrukerDto(
    val personIdent: String,
) {
    init {
        Fødselsnummer(personIdent)
    }
}
