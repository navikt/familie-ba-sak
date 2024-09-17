package no.nav.familie.ba.sak.integrasjoner.oppgave

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import org.springframework.stereotype.Service

private const val MIDLERTIDIG_ENHET_4863 = "4863"
private const val VIKAFOSSEN_ENHET_2103_ID = "2103"
private const val VIKAFOSSEN_ENHET_2103_NAVN = "NAV Vikafossen"

@Service
class NavIdentOgEnhetsnummerService(
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    private val integrasjonClient: IntegrasjonClient,
) {
    fun hentNavIdentOgEnhetsnummer(
        behandlingId: Long,
        navIdent: String?,
    ): NavIdentOgEnhet {
        val behandlendeEnhet =
            arbeidsfordelingPåBehandlingRepository
                .hentArbeidsfordelingPåBehandling(behandlingId)
        return when (behandlendeEnhet.behandlendeEnhetId) {
            MIDLERTIDIG_ENHET_4863 -> håndterMidlertidigEnhet4863(navIdent)
            VIKAFOSSEN_ENHET_2103_ID -> håndterVikafossenEnhet2103(navIdent)
            else -> håndterAndreEnheter(navIdent, behandlendeEnhet)
        }
    }

    private fun håndterMidlertidigEnhet4863(
        navIdent: String?,
    ): NavIdentOgEnhet {
        if (navIdent == null) {
            throw Feil("Kan ikke sette midlertidig enhet 4863 om man mangler NAV-ident")
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { it.enhetsnummer != MIDLERTIDIG_ENHET_4863 }
                .filter { it.enhetsnummer != VIKAFOSSEN_ENHET_2103_ID }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
        return NavIdentOgEnhet(navIdent, nyBehandlendeEnhet.enhetsnummer, nyBehandlendeEnhet.enhetsnavn)
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: String?,
    ): NavIdentOgEnhet {
        if (navIdent == null) {
            throw Feil("Kan ikke sette Vikafossen enhet 2103 om man mangler NAV-ident")
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { it.enhetsnummer != MIDLERTIDIG_ENHET_4863 }
        val harTilgangTilVikafossenEnhet2103 =
            enheterNavIdentHarTilgangTil
                .any { it.enhetsnummer == VIKAFOSSEN_ENHET_2103_ID }
        if (!harTilgangTilVikafossenEnhet2103) {
            return NavIdentOgEnhet(null, VIKAFOSSEN_ENHET_2103_ID, VIKAFOSSEN_ENHET_2103_NAVN)
        }
        return NavIdentOgEnhet(navIdent, VIKAFOSSEN_ENHET_2103_ID, VIKAFOSSEN_ENHET_2103_NAVN)
    }

    private fun håndterAndreEnheter(
        navIdent: String?,
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
    ): NavIdentOgEnhet {
        if (navIdent == null) {
            // navIdent er null ved automatisk journalføring
            return NavIdentOgEnhet(null, arbeidsfordelingPåBehandling.behandlendeEnhetId, arbeidsfordelingPåBehandling.behandlendeEnhetNavn)
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { it.enhetsnummer != MIDLERTIDIG_ENHET_4863 }
                .filter { it.enhetsnummer != VIKAFOSSEN_ENHET_2103_ID }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }
        val harTilgangTilBehandledeEnhet = enheterNavIdentHarTilgangTil.any { it.enhetsnummer == arbeidsfordelingPåBehandling.behandlendeEnhetId }
        if (!harTilgangTilBehandledeEnhet) {
            // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
            val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
            return NavIdentOgEnhet(navIdent, nyBehandlendeEnhet.enhetsnummer, nyBehandlendeEnhet.enhetsnavn)
        }
        return NavIdentOgEnhet(navIdent, arbeidsfordelingPåBehandling.behandlendeEnhetId, arbeidsfordelingPåBehandling.behandlendeEnhetNavn)
    }
}

data class NavIdentOgEnhet(
    val navIdent: String?,
    val enhetsnummer: String,
    val enhetsnavn: String,
) {
    init {
        if (enhetsnummer.length != 4) {
            throw IllegalArgumentException("Enhetsnummer må være 4 siffer")
        }
    }
}
