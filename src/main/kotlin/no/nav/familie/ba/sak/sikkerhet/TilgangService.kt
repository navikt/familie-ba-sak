package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val rolleConfig: RolleConfig,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val auditLogger: AuditLogger,
) {
    /**
     * Sjekk om saksbehandler har tilgang til å gjøre en gitt handling.
     *
     * @minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @handling kort beskrivelse for handlingen. Eksempel: 'endre vilkår', 'oppprette behandling'.
     * Handlingen kommer til saksbehandler så det er viktig at denne gir mening.
     */
    fun verifiserHarTilgangTilHandling(
        minimumBehandlerRolle: BehandlerRolle,
        handling: String,
    ) {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)
        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw RolleTilgangskontrollFeil(
                melding =
                    "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang " +
                        "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling.",
            )
        }
    }

    fun validerTilgangTilPersoner(
        personIdenter: List<String>,
        event: AuditLoggerEvent,
    ) {
        personIdenter.forEach { auditLogger.log(Sporingsdata(event, it)) }
        if (!harTilgangTilPersoner(personIdenter)) {
            throw RolleTilgangskontrollFeil(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang.",
                frontendFeilmelding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til $personIdenter",
            )
        }
    }

    /**
     * sjekkTilgangTilPersoner er cachet i [familieIntegrasjonerTilgangskontrollService]
     */
    private fun harTilgangTilPersoner(personIdenter: List<String>): Boolean =
        familieIntegrasjonerTilgangskontrollService
            .sjekkTilgangTilPersoner(personIdenter)
            .all { it.value.harTilgang }

    fun validerTilgangTilBehandling(
        behandlingId: Long,
        event: AuditLoggerEvent,
    ) {
        val personIdenter =
            persongrunnlagService
                .hentSøkerOgBarnPåBehandling(behandlingId)
                ?.map { it.aktør.aktivFødselsnummer() }
                ?: listOf(
                    behandlingHentOgPersisterService
                        .hent(behandlingId)
                        .fagsak.aktør
                        .aktivFødselsnummer(),
                )

        if (!SikkerhetContext.erSystemKontekst()) {
            personIdenter.forEach {
                auditLogger.log(
                    Sporingsdata(
                        event = event,
                        personIdent = it,
                        custom1 = CustomKeyValue("behandling", behandlingId.toString()),
                    ),
                )
            }
        }

        if (!harTilgangTilPersoner(personIdenter)) {
            throw RolleTilgangskontrollFeil(
                "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                    "har ikke tilgang til behandling=$behandlingId",
            )
        }
    }

    fun validerTilgangTilFagsak(
        fagsakId: Long,
        event: AuditLoggerEvent,
    ) {
        val aktør = fagsakService.hentAktør(fagsakId)
        val personIdenterIFagsak =
            (
                persongrunnlagService
                    .hentSøkerOgBarnPåFagsak(fagsakId)
                    ?.map { it.aktør.aktivFødselsnummer() }
                    ?: emptyList()
            ).ifEmpty { listOf(aktør.aktivFødselsnummer()) }

        personIdenterIFagsak.forEach { fnr ->
            auditLogger.log(
                Sporingsdata(
                    event = event,
                    personIdent = fnr,
                    custom1 = CustomKeyValue("fagsak", fagsakId.toString()),
                ),
            )
        }
        val harTilgang = harTilgangTilPersoner(personIdenterIFagsak)
        if (!harTilgang) {
            throw RolleTilgangskontrollFeil(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til fagsak=$fagsakId.",
                frontendFeilmelding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til fagsak=$fagsakId.",
            )
        }
    }

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre bestemt handling og om saksbehandler kan behandle fagsak
     * @param fagsakId id til fagsak det skal sjekkes tilgang til
     * @param event operasjon som skal gjøres med identene
     * @param minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @param handling kort beskrivelse for handlingen.
     */
    fun validerTilgangTilHandlingOgFagsak(
        fagsakId: Long,
        event: AuditLoggerEvent,
        minimumBehandlerRolle: BehandlerRolle,
        handling: String,
    ) {
        verifiserHarTilgangTilHandling(minimumBehandlerRolle, handling)
        validerTilgangTilFagsak(fagsakId, event)
    }

    fun validerKanRedigereBehandling(behandlingId: Long) {
        validerBehandlingKanRedigeres(behandlingHentOgPersisterService.hentStatus(behandlingId))
    }

    fun validerErPåBeslutteVedtakSteg(behandlingId: Long) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        if (behandling.status != BehandlingStatus.FATTER_VEDTAK && behandling.steg == StegType.BESLUTTE_VEDTAK) {
            throw Feil(message = "Er ikke i riktig steg eller status. Forventer status FATTER_VEDTAK og steg BESLUTTE_VEDTAK")
        }
    }
}
