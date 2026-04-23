package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.common.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val rolleConfig: RolleConfig,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val auditLogger: AuditLogger,
    private val strengtFortroligService: StrengtFortroligService,
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
        // Hvis minimumBehandlerRolle er forvalter, må innlogget bruker ha FORVALTER rolle
        if (minimumBehandlerRolle == BehandlerRolle.FORVALTER &&
            !SikkerhetContext.harInnloggetBrukerForvalterRolle(rolleConfig)
        ) {
            throw RolleTilgangskontrollFeil(
                melding =
                    "${SikkerhetContext.hentSaksbehandlerNavn()} " +
                        "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle",
            )
        }

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
        begrunnelse: String? = null,
    ) {
        personIdenter.forEach {
            auditLogger.log(Sporingsdata(event = event, personIdent = it, msg = begrunnelse))
        }
        val tilgangerTilPersoner = sjekkTilgangTilPersoner(personIdenter)
        if (!tilgangerTilPersoner.all { it.harTilgang }) {
            val adressebeskyttelsegraderingEllerNavAnsatt = tilgangerTilPersoner.tilBegrunnelserForManglendeTilgang()
            throw RolleTilgangskontrollFeil(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang. $adressebeskyttelsegraderingEllerNavAnsatt.",
                frontendFeilmelding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang. $adressebeskyttelsegraderingEllerNavAnsatt.",
            )
        }
    }

    /**
     * sjekkTilgangTilPersoner er cachet i [familieIntegrasjonerTilgangskontrollService]
     */
    private fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> =
        familieIntegrasjonerTilgangskontrollService
            .sjekkTilgangTilPersoner(personIdenter)
            .map { it.value }

    fun validerTilgangTilBehandling(
        behandlingId: Long,
        event: AuditLoggerEvent,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val personerPåBehandling = persongrunnlagService.hentSøkerOgBarnPåBehandling(behandlingId)
        val søker = behandling.fagsak.skjermetBarnSøker?.aktør ?: behandling.fagsak.aktør

        val personIdenter =
            personerPåBehandling
                ?.map { it.aktør.aktivFødselsnummer() }
                ?: listOf(behandling.fagsak.aktør.aktivFødselsnummer())

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

        val tilgangerTilPersoner = sjekkTilgangTilPersoner(personIdenter)

        if (!strengtFortroligService.harTilgangTilAllePersonerEllerKunManglendeTilgangTilSkjermedeBarnUtenLøpendeAndeler(behandling.fagsak.id, personIdenter, søker)) {
            val adressebeskyttelsegraderingEllerNavAnsatt = tilgangerTilPersoner.tilBegrunnelserForManglendeTilgang()
            throw RolleTilgangskontrollFeil(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til behandling=$behandlingId. $adressebeskyttelsegraderingEllerNavAnsatt.",
                frontendFeilmelding = "Behandlingen inneholder personer som krever ytterligere tilganger. $adressebeskyttelsegraderingEllerNavAnsatt.",
            )
        }
    }

    fun validerTilgangTilFagsak(
        fagsakId: Long,
        event: AuditLoggerEvent,
    ) {
        val aktør = fagsakService.hentAktør(fagsakId)
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)
        val søker = fagsak.skjermetBarnSøker?.aktør ?: fagsak.aktør

        val personerPåFagsak = persongrunnlagService.hentSøkerOgBarnPåFagsak(fagsakId)
        val personIdenterIFagsak =
            (
                personerPåFagsak
                    ?.map { it.aktør.aktivFødselsnummer() }
                    ?: emptyList()
            ).ifEmpty {
                listOfNotNull(aktør.aktivFødselsnummer(), fagsak.skjermetBarnSøker?.aktør?.aktivFødselsnummer())
            }

        personIdenterIFagsak.forEach { fnr ->
            auditLogger.log(
                Sporingsdata(
                    event = event,
                    personIdent = fnr,
                    custom1 = CustomKeyValue("fagsak", fagsakId.toString()),
                ),
            )
        }

        val tilgangerTilPersoner = sjekkTilgangTilPersoner(personIdenterIFagsak)

        if (!strengtFortroligService.harTilgangTilAllePersonerEllerKunManglendeTilgangTilSkjermedeBarnUtenLøpendeAndeler(fagsak.id, personIdenterIFagsak, søker)) {
            val adressebeskyttelsegraderingEllerNavAnsatt = tilgangerTilPersoner.tilBegrunnelserForManglendeTilgang()
            throw RolleTilgangskontrollFeil(
                melding =
                    "Saksbehandler ${SikkerhetContext.hentSaksbehandler()} " +
                        "har ikke tilgang til fagsak=$fagsakId. $adressebeskyttelsegraderingEllerNavAnsatt.",
                frontendFeilmelding =
                    "Fagsaken inneholder personer som krever ytterligere tilganger. $adressebeskyttelsegraderingEllerNavAnsatt.",
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

    private fun List<Tilgang>.tilBegrunnelserForManglendeTilgang(): String =
        this
            .filter { !it.harTilgang }
            .mapNotNull { it.begrunnelse }
            .toSet()
            .toList()
            .slåSammen()
}
