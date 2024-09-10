package no.nav.familie.ba.sak.integrasjoner.oppgave

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import org.springframework.stereotype.Service

@Service
class EnhetsnummerService(
    val integrasjonClient: IntegrasjonClient,
) {
    fun hentEnhetsnummer(
        behandlendeEnhetId: String,
        navIdent: String?,
    ): String {
        if (navIdent == null) {
            when (behandlendeEnhetId) {
                "4863" -> throw Feil("")
                else -> behandlendeEnhetId
            }
            return behandlendeEnhetId
        }

        val enhetTilgang = integrasjonClient.hentEnhetTilgang(navIdent)
        val enheterNavIdentenHarTilgangTil = enhetTilgang.enheter.map { it.enhetsnummer }
        if (enheterNavIdentenHarTilgangTil.isEmpty()) {
            throw Feil("Nav-ident har ikke tilgang til noen enheter")
        }
        return enheterNavIdentenHarTilgangTil.singleOrNull { it == behandlendeEnhetId } ?: enheterNavIdentenHarTilgangTil.first()
    }
}
