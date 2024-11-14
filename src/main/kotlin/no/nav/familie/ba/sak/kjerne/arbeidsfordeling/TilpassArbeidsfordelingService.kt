package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MidlertidigEnhetIAutomatiskBehandlingFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.kontrakter.felles.NavIdent
import org.springframework.stereotype.Service

@Service
class TilpassArbeidsfordelingService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun tilpassArbeidsfordelingsenhetTilSaksbehandler(
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet =
        when (arbeidsfordelingsenhet.enhetId) {
            BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer -> håndterMidlertidigEnhet4863(navIdent)
            BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer -> håndterVikafossenEnhet2103(navIdent)
            else -> håndterAndreEnheter(navIdent, arbeidsfordelingsenhet)
        }

    fun bestemTilordnetRessursPåOppgave(
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
        navIdent: NavIdent?,
    ): NavIdent? {
        if (navIdent?.erSystemIdent() == true) {
            return null
        }
        return if (harSaksbehandlerTilgangTilEnhet(enhetId = arbeidsfordelingsenhet.enhetId, navIdent = navIdent)) {
            navIdent
        } else {
            null
        }
    }

    private fun harSaksbehandlerTilgangTilEnhet(
        enhetId: String,
        navIdent: NavIdent?,
    ): Boolean =
        navIdent?.let {
            integrasjonClient
                .hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
                .any { it.enhetsnummer == enhetId }
        } ?: false

    private fun håndterMidlertidigEnhet4863(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            throw Feil("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident")
        }
        if (navIdent.erSystemIdent()) {
            throw MidlertidigEnhetIAutomatiskBehandlingFeil("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} i automatiske behandlinger")
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
                .filter { it.enhetsnummer != BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
        return Arbeidsfordelingsenhet(
            nyBehandlendeEnhet.enhetsnummer,
            nyBehandlendeEnhet.enhetsnavn,
        )
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            throw Feil("Kan ikke håndtere ${BarnetrygdEnhet.VIKAFOSSEN} om man mangler NAV-ident")
        }
        return Arbeidsfordelingsenhet(
            BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
            BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
        )
    }

    private fun håndterAndreEnheter(
        navIdent: NavIdent?,
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null || navIdent.erSystemIdent()) {
            // navIdent er null ved automatisk journalføring
            return Arbeidsfordelingsenhet(
                arbeidsfordelingsenhet.enhetId,
                arbeidsfordelingsenhet.enhetNavn,
            )
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
                .filter { it.enhetsnummer != BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }
        val harTilgangTilBehandledeEnhet =
            enheterNavIdentHarTilgangTil.any {
                it.enhetsnummer == arbeidsfordelingsenhet.enhetId
            }
        if (!harTilgangTilBehandledeEnhet) {
            // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
            val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
            return Arbeidsfordelingsenhet(
                nyBehandlendeEnhet.enhetsnummer,
                nyBehandlendeEnhet.enhetsnavn,
            )
        }
        return Arbeidsfordelingsenhet(
            arbeidsfordelingsenhet.enhetId,
            arbeidsfordelingsenhet.enhetNavn,
        )
    }

    private fun NavIdent.erSystemIdent(): Boolean = this.ident == SYSTEM_FORKORTELSE
}
