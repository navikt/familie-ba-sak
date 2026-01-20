package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.kontrakter.felles.Fødselsnummer

data class SkjermetBarnSøkerDto(
    val søkersIdent: String,
) {
    fun valider() {
        try {
            Fødselsnummer(søkersIdent)
        } catch (exception: Exception) {
            throw FunksjonellFeil("Ugyldig fødselsnummer.")
        }
    }
}
