package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.erAlfanummeriskPlussKolon
import no.nav.familie.ba.sak.common.secureLogger

data class NavnOgIdent(
    val navn: String,
    val id: String,
) {
    // Bruker init til å validere personidenten
    init {
        if (!id.erAlfanummeriskPlussKolon()) {
            secureLogger.info("Ugyldig ident: $id")
            throw FunksjonellFeil(
                melding = "Ugyldig ident. Se securelog for mer informasjon.",
                frontendFeilmelding = "Ugyldig ident. Normalt et fødselsnummer eller organisasjonsnummer",
            )
        }
    }
}
