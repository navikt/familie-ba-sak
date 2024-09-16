package no.nav.familie.ba.sak.integrasjoner.oppgave

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import org.springframework.stereotype.Service

private const val MIDLERTIDIG_ENHET_4863 = "4863"
private const val VIKAFOSSEN_ENHET_2103 = "2103"

@Service
class NavIdentOgEnhetsnummerService(
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    private val integrasjonClient: IntegrasjonClient,
) {
    fun hentNavIdentOgEnhetsnummer(
        behandlingId: Long,
        navIdent: String?,
    ): NavIdentOgEnhetsnummer {
        val behandlendeEnhetId =
            arbeidsfordelingPåBehandlingRepository
                .hentArbeidsfordelingPåBehandling(behandlingId)
                .behandlendeEnhetId
        return when (behandlendeEnhetId) {
            MIDLERTIDIG_ENHET_4863 -> håndterMidlertidigEnhet4863(navIdent)
            VIKAFOSSEN_ENHET_2103 -> håndterVikafossenEnhet2103(navIdent)
            else -> håndterAndreEnheter(navIdent, behandlendeEnhetId)
        }
    }

    private fun håndterMidlertidigEnhet4863(
        navIdent: String?,
    ): NavIdentOgEnhetsnummer {
        if (navIdent == null) {
            throw Feil("Kan ikke sette midlertidig enhet 4863 om man mangler NAV-ident")
        }
        val enhetsnummerSaksbehandlerHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .map { it.enhetsnummer }
                .filter { it != MIDLERTIDIG_ENHET_4863 }
                .filter { it != VIKAFOSSEN_ENHET_2103 }
        if (enhetsnummerSaksbehandlerHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        return NavIdentOgEnhetsnummer(navIdent, enhetsnummerSaksbehandlerHarTilgangTil.first())
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: String?,
    ): NavIdentOgEnhetsnummer {
        if (navIdent == null) {
            throw Feil("Kan ikke sette Vikafossen enhet 2103 om man mangler NAV-ident")
        }
        val harTilgangTilVikafossenEnhet2103 =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .any { it.enhetsnummer == VIKAFOSSEN_ENHET_2103 }
        if (!harTilgangTilVikafossenEnhet2103) {
            return NavIdentOgEnhetsnummer(null, VIKAFOSSEN_ENHET_2103)
        }
        return NavIdentOgEnhetsnummer(navIdent, VIKAFOSSEN_ENHET_2103)
    }

    private fun håndterAndreEnheter(
        navIdent: String?,
        behandlendeEnhetId: String,
    ): NavIdentOgEnhetsnummer {
        if (navIdent == null) {
            // navIdent er null ved automatisk journalføring
            return NavIdentOgEnhetsnummer(null, behandlendeEnhetId)
        }
        val enheterNavIdentHarTilgangerTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .map { it.enhetsnummer }
                .filter { it != MIDLERTIDIG_ENHET_4863 }
                .filter { it != VIKAFOSSEN_ENHET_2103 }
        if (enheterNavIdentHarTilgangerTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }
        val harTilgangTilBehandledeEnhet = enheterNavIdentHarTilgangerTil.contains(behandlendeEnhetId)
        if (!harTilgangTilBehandledeEnhet) {
            // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
            return NavIdentOgEnhetsnummer(navIdent, enheterNavIdentHarTilgangerTil.first())
        }
        return NavIdentOgEnhetsnummer(navIdent, behandlendeEnhetId)
    }
}

data class NavIdentOgEnhetsnummer(
    val navIdent: String?,
    val enhetsnummer: String,
) {
    init {
        if (enhetsnummer.length != 4) {
            throw IllegalArgumentException("Enhetsnummer må være 4 siffer")
        }
    }
}
