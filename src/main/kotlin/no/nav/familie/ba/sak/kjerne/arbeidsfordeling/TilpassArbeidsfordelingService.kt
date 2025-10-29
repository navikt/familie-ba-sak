package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.containsExactly
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.kontrakter.felles.NavIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TilpassArbeidsfordelingService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    private val logger = LoggerFactory.getLogger(TilpassArbeidsfordelingService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

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
            integrasjonKlient
                .hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
                .any { it.enhetsnummer == enhetId }
        } ?: false

    private fun håndterMidlertidigEnhet4863(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            logger.error("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident.")
            throw Feil("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident.")
        }
        if (navIdent.erSystemIdent()) {
            logger.error("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} i automatiske behandlinger.")
            throw MidlertidigEnhetIAutomatiskBehandlingFeil("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} i automatiske behandlinger.")
        }
        val enheterNavIdentHarTilgangTil = integrasjonKlient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            logger.warn("Nav-Ident har ikke tilgang til noen enheter. Se SecureLogs for detaljer.")
            secureLogger.warn("Nav-Ident $navIdent har ikke tilgang til noen enheter.")
            throw FunksjonellFeil("Nav-Ident har ikke tilgang til noen enheter.")
        }
        val navIdentHarKunTilgangTilVikafossen = enheterNavIdentHarTilgangTil.map { it.enhetsnummer }.containsExactly(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
        if (navIdentHarKunTilgangTilVikafossen) {
            // Skal kun være lovt til å sette Vikafossen når det er eneste valgmulighet
            return Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.VIKAFOSSEN)
        }
        val enheterNavIdentHarTilgangTilForutenVikafossen = enheterNavIdentHarTilgangTil.filter { it.enhetsnummer != BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        val nyBehandlendeEnhet = enheterNavIdentHarTilgangTilForutenVikafossen.first()
        return Arbeidsfordelingsenhet.opprettFra(nyBehandlendeEnhet)
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            logger.error("Kan ikke håndtere ${BarnetrygdEnhet.VIKAFOSSEN} om man mangler NAV-ident.")
            throw Feil("Kan ikke håndtere ${BarnetrygdEnhet.VIKAFOSSEN} om man mangler NAV-ident.")
        }
        return Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.VIKAFOSSEN)
    }

    private fun håndterAndreEnheter(
        navIdent: NavIdent?,
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null || navIdent.erSystemIdent()) {
            // navIdent er null ved automatisk journalføring
            return arbeidsfordelingsenhet
        }
        val enheterNavIdentHarTilgangTil = integrasjonKlient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            logger.warn("Nav-Ident har ikke tilgang til noen enheter. Se SecureLogs for detaljer.")
            secureLogger.warn("Nav-Ident $navIdent har ikke tilgang til noen enheter.")
            throw FunksjonellFeil("Nav-Ident har ikke tilgang til noen enheter.")
        }
        val navIdentHarKunTilgangTilVikafossen = enheterNavIdentHarTilgangTil.map { it.enhetsnummer }.containsExactly(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
        if (navIdentHarKunTilgangTilVikafossen) {
            // Skal kun være lovt til å sette Vikafossen når det er eneste valgmulighet
            return Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.VIKAFOSSEN)
        }
        val enheterNavIdentHarTilgangTilForutenVikafossen = enheterNavIdentHarTilgangTil.filter { it.enhetsnummer != BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer }
        val harTilgangTilBehandledeEnhet = enheterNavIdentHarTilgangTilForutenVikafossen.any { it.enhetsnummer == arbeidsfordelingsenhet.enhetId }
        if (!harTilgangTilBehandledeEnhet) {
            // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
            val nyBehandlendeEnhet = enheterNavIdentHarTilgangTilForutenVikafossen.first()
            return Arbeidsfordelingsenhet.opprettFra(nyBehandlendeEnhet)
        }
        return arbeidsfordelingsenhet
    }

    private fun NavIdent.erSystemIdent(): Boolean = this.ident == SYSTEM_FORKORTELSE
}
