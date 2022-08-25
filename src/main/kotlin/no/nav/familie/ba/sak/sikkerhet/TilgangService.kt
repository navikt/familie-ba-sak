package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.CustomKeyValue
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.Sporingsdata
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val rolleConfig: RolleConfig,
    private val familieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
    private val cacheManager: CacheManager,
    private val auditLogger: AuditLogger
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
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang " +
                    "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling."
            )
        }
    }

    fun validerTilgangTilPersoner(personIdenter: List<String>, event: AuditLoggerEvent) {
        personIdenter.forEach { auditLogger.log(Sporingsdata(event, it)) }
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
            familieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(personIdenter).harTilgang
        }
    }

    fun validerTilgangTilBehandling(behandlingId: Long, event: AuditLoggerEvent) {
        val harTilgang = harSaksbehandlerTilgang("validerTilgangTilBehandling", behandlingId) {
            val behandling = behandlingHentOgPersisterService.hent(behandlingId)
            val personIdenter =
                persongrunnlagService.hentAktiv(behandlingId = behandlingId)?.søkerOgBarn?.map { it.aktør.aktivFødselsnummer() }
                    ?: listOf(behandling.fagsak.aktør.aktivFødselsnummer())
            personIdenter.forEach {
                auditLogger.log(
                    Sporingsdata(
                        event = event,
                        personIdent = it,
                        custom1 = CustomKeyValue("behandling", behandlingId.toString())
                    )
                )
            }
            harTilgangTilPersoner(personIdenter)
        }
        if (!harTilgang) {
            throw RolleTilgangskontrollFeil(
                "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til behandling=$behandlingId"
            )
        }
    }

    fun validerTilgangTilFagsak(fagsakId: Long, event: AuditLoggerEvent) {
        val aktør = fagsakService.hentAktør(fagsakId)
        aktør.personidenter.forEach {
            Sporingsdata(
                event = event,
                personIdent = it.fødselsnummer,
                custom1 = CustomKeyValue("fagsak", fagsakId.toString())
            )
        }
        val behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsakId)
        val personIdenterIFagsak = behandlinger.flatMap { behandling ->
            val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
            when {
                personopplysningGrunnlag != null -> personopplysningGrunnlag.søkerOgBarn.map { person -> person.aktør.aktivFødselsnummer() }
                else -> emptyList()
            }
        }.distinct().ifEmpty { listOf(aktør.aktivFødselsnummer()) }
        val harTilgang = harTilgangTilPersoner(personIdenterIFagsak)
        if (!harTilgang) {
            throw RolleTilgangskontrollFeil(
                melding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til fagsak=$fagsakId.",
                frontendFeilmelding = "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til fagsak=$fagsakId."
            )
        }
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
