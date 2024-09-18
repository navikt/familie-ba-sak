package no.nav.familie.ba.sak.integrasjoner.oppgave

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.Companion.erGyldigBehandlendeBarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.kontrakter.felles.NavIdent
import org.springframework.stereotype.Service

@Service
class NavIdentOgEnhetService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun hentNavIdentOgEnhet(
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        navIdent: NavIdent?,
    ): NavIdentOgEnhet =
        when (arbeidsfordelingPåBehandling.behandlendeEnhetId) {
            BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer -> håndterMidlertidigEnhet4863(navIdent)
            BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer -> håndterVikafossenEnhet2103(navIdent)
            else -> håndterAndreEnheter(navIdent, arbeidsfordelingPåBehandling)
        }

    private fun håndterMidlertidigEnhet4863(
        navIdent: NavIdent?,
    ): NavIdentOgEnhet {
        if (navIdent == null) {
            throw Feil("Kan ikke sette midlertidig enhet 4863 om man mangler NAV-ident")
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { erGyldigBehandlendeBarnetrygdEnhet(it.enhetsnummer) }
                .filter { it.enhetsnummer != BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
        return NavIdentOgEnhet(navIdent, nyBehandlendeEnhet.enhetsnummer, nyBehandlendeEnhet.enhetsnavn)
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: NavIdent?,
    ): NavIdentOgEnhet {
        if (navIdent == null) {
            throw Feil("Kan ikke sette Vikafossen enhet 2103 om man mangler NAV-ident")
        }
        val harTilgangTilVikafossenEnhet2103 =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { erGyldigBehandlendeBarnetrygdEnhet(it.enhetsnummer) }
                .any { it.enhetsnummer == BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer }
        if (!harTilgangTilVikafossenEnhet2103) {
            return NavIdentOgEnhet(null, BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer, BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn)
        }
        return NavIdentOgEnhet(navIdent, BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer, BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn)
    }

    private fun håndterAndreEnheter(
        navIdent: NavIdent?,
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
    ): NavIdentOgEnhet {
        if (navIdent == null) {
            // navIdent er null ved automatisk journalføring
            return NavIdentOgEnhet(null, arbeidsfordelingPåBehandling.behandlendeEnhetId, arbeidsfordelingPåBehandling.behandlendeEnhetNavn)
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { erGyldigBehandlendeBarnetrygdEnhet(it.enhetsnummer) }
                .filter { it.enhetsnummer != BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer }
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
    val navIdent: NavIdent?,
    val enhetsnummer: String,
    val enhetsnavn: String,
) {
    init {
        if (enhetsnummer.length != 4) {
            throw IllegalArgumentException("Enhetsnummer må være 4 siffer")
        }
    }
}
