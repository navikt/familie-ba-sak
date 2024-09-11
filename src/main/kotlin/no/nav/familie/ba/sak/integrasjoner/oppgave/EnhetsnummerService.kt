package no.nav.familie.ba.sak.integrasjoner.oppgave

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import org.springframework.stereotype.Service

@Service
class EnhetsnummerService(
    val integrasjonClient: IntegrasjonClient,
) {
    fun hentEnhetsnummer(
        behandlendeEnhetIdFraArbeisfordeling: String,
        navIdent: String?,
    ): String {
        when (behandlendeEnhetIdFraArbeisfordeling) {
            "4863" -> håndter4863(navIdent)
            "2103" -> håndter2103(navIdent)
            else -> {}
        }

        if (navIdent == null) {
            return behandlendeEnhetIdFraArbeisfordeling
        }
        val enhetTilgang = integrasjonClient.hentEnhetTilgang(navIdent)
        val enheterNavIdentenHarTilgangTil = enhetTilgang.enheter
            .map { it.enhetsnummer }
            .filter { it != "4863" }
        if (enheterNavIdentenHarTilgangTil.isEmpty()) {
            throw Feil("Nav-ident har ikke tilgang til noen enheter")
        }
        return enheterNavIdentenHarTilgangTil.singleOrNull { it == behandlendeEnhetIdFraArbeisfordeling } ?: enheterNavIdentenHarTilgangTil.first()
    }

    private fun håndter4863(navIdent: String?): String {
        if (navIdent == null) {
            throw Feil("Kan ikke sette 4863 om man mangler saksbehandler")
        }
        val enhetTilgang = integrasjonClient.hentEnhetTilgang(navIdent)
        val enhetNavIdentenHarTilgangTil = enhetTilgang.enheter
            .map { it.enhetsnummer }
            .firstOrNull { it != "4863" }
        if (enhetNavIdentenHarTilgangTil == null) {
            throw Feil("Fant ingen enhet for Nav-ident $navIdent")
        }
        return enhetNavIdentenHarTilgangTil
    }

    private fun håndter2103(navIdent: String?): String {
        if (navIdent == null) {
            return "2103"
        }
        // TODO : Implementer dette
        return ""
    }
}

data class NavIdentOgTilhørendeEnhetsnummer(
    val tilordnetRessursId: String,
    val enhetsnummer: String?
)
