package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val rolleConfig: RolleConfig,
    private val integrasjonClient: IntegrasjonClient,
    private val cacheManager: CacheManager
) {

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre en gitt handling.
     *
     * @minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @handling kort beskrivelse for handlingen. Eksempel: 'endre vilkår', 'oppprette behandling'.
     * Handlingen kommer til saksbehandler så det er viktig at denne gir mening.
     */
    fun verifiserHarTilgangTilHandling(minimumBehandlerRolle: BehandlerRolle, handling: String) {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw RolleTilgangskontrollFeil(
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling."
            )
        }
    }

    fun validerTilgangTilPersoner(personIdenter: List<String>) {
        if (!harTilgangTilPersoner(personIdenter)) {
            throw RolleTilgangskontrollFeil(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang.",
                frontendFeilmelding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til $personIdenter"
            )
        }
    }

    private fun harTilgangTilPersoner(personIdenter: List<String>): Boolean {
        return harSaksbehandlerTilgang("validerTilgangTilPersoner", personIdenter) {
            integrasjonClient.sjekkTilgangTilPersoner(personIdenter).harTilgang
        }
    }

    fun validerTilgangTilBehandling(behandlingId: Long) {
        val harTilgang = harSaksbehandlerTilgang("validerTilgangTilBehandling", behandlingId) {
            val personIdenter =
                persongrunnlagService.hentAktivThrows(behandlingId = behandlingId).personer.map { it.personIdent.ident }
            harTilgangTilPersoner(personIdenter)
        }
        if (!harTilgang) {
            throw RolleTilgangskontrollFeil(
                "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til behandling=$behandlingId",
            )
        }
    }

    fun validerTilgangTilFagsak(fagsakId: Long) {
        val personIdent = fagsakService.hentAktør(fagsakId).aktivFødselsnummer()
        validerTilgangTilPersoner(listOf(personIdent))
    }

    /**
     * Sjekker cache om tilgangen finnes siden tidligere, hvis ikke hentes verdiet med [hentVerdi]
     * Resultatet caches sammen med identen for saksbehandleren på gitt [cacheName]
     * @param cacheName navnet på cachen
     * @param verdi verdiet som man ønsket å hente cache for, eks behandlingId, eller personIdent
     */
    private fun <T> harSaksbehandlerTilgang(cacheName: String, verdi: T, hentVerdi: () -> Boolean): Boolean {
        if (SikkerhetContext.erSystemKontekst()) return true

        val cache = cacheManager.getCache(cacheName) ?: error("Finner ikke cache=$cacheName")
        return cache.get(Pair(verdi, SikkerhetContext.hentSaksbehandler())) {
            hentVerdi()
        } ?: error("Finner ikke verdi fra cache=$cacheName")
    }
}
