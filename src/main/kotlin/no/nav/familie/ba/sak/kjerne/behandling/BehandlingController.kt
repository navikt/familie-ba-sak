package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.ok
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.kjerne.simulering.vedtakSimuleringMottakereTilRestSimulering
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.sikkerhet.validering.BehandlingstilgangConstraint
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val fagsakService: FagsakService,
    private val stegService: StegService,
    private val behandlingsService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilgangService: TilgangService,
    private val simuleringService: SimuleringService,
) {

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette behandling"
        )

        if (nyBehandling.søkersIdent.isBlank()) {
            throw Feil(
                message = "Søkers ident kan ikke være blank",
                frontendFeilmelding = "Klarte ikke å opprette behandling. Mangler ident på bruker."
            )
        }

        if (nyBehandling.behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD && nyBehandling.barnasIdenter.isEmpty()) {
            throw Feil(
                message = "Listen med barn er tom ved opprettelse av migreringsbehandling",
                frontendFeilmelding = "Klarte ikke å opprette behandling. Mangler barna det gjelder."
            )
        }

        return Result.runCatching {
            stegService.håndterNyBehandling(nyBehandling)
        }.fold(
            onSuccess = {
                val restFagsak = ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.fagsak.id))
                restFagsak
            },
            onFailure = {
                throw it
            }
        )
    }

    @PutMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettEllerOppdaterBehandlingFraHendelse(
        @RequestBody
        nyBehandling: NyBehandlingHendelse
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SYSTEM,
            handling = "opprette behandling fra hendelse"
        )

        return try {
            val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandling))
            taskRepository.save(task)
            ok("Task opprettet for behandling av fødselshendelse.")
        } catch (ex: Throwable) {
            illegalState("Task kunne ikke opprettes for behandling av fødselshendelse: ${ex.message}", ex)
        }
    }

    @PutMapping(path = ["{behandlingId}/henlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun henleggBehandlingOgSendBrev(
        @PathVariable(name = "behandlingId") behandlingId: Long,
        @RequestBody henleggInfo: RestHenleggBehandlingInfo
    ): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "henlegge behandling"
        )

        val behandling = behandlingsService.hent(behandlingId)
        val response = stegService.håndterHenleggBehandling(behandling, henleggInfo)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = response.fagsak.id))
    }

    @GetMapping(path = ["/{behandlingId}/simulering"])
    fun hentSimulering(
        @PathVariable @BehandlingstilgangConstraint
        behandlingId: Long
    ): ResponseEntity<Ressurs<RestSimulering>> {
        val vedtakSimuleringMottaker = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottaker)
        return ResponseEntity.ok(Ressurs.success(restSimulering))
    }

    @PutMapping(path = ["/{behandlingId}/behandlingstema"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreBehandlingstema(
        @PathVariable behandlingId: Long,
        @RequestBody
        endreBehandling: RestEndreBehandlingstema
    ): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingsService.oppdaterBehandlingstema(
            behandling = behandlingsService.hent(behandlingId),
            nyBehandlingUnderkategori = endreBehandling.behandlingUnderkategori,
            nyBehandlingKategori = endreBehandling.behandlingKategori,
            manueltOppdatert = true
        )

        val restFagsak = fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id)

        return ResponseEntity.ok(restFagsak)
    }

    @Transactional
    @PostMapping(path = ["/{behandlingId}/tilbakekreving"])
    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
        @PathVariable behandlingId: Long,
        @RequestBody restTilbakekreving: RestTilbakekreving?
    ): ResponseEntity<Ressurs<RestFagsak>> {

        val behandling = behandlingsService.hent(behandlingId)
        stegService.håndterVurderTilbakekreving(behandling, restTilbakekreving)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PostMapping(path = ["/{behandlingId}/steg/behandlingsresultat"])
    fun utledBehandlingsresultat(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingsService.hent(behandlingId)
        stegService.håndterBehandlingsresultat(behandling)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }
}

data class NyBehandling(
    val kategori: BehandlingKategori?,
    val underkategori: BehandlingUnderkategori?,
    val søkersIdent: String,
    val behandlingType: BehandlingType,
    val journalpostID: String? = null,
    val behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    val skalBehandlesAutomatisk: Boolean = false,
    val navIdent: String? = null,
    val barnasIdenter: List<String> = emptyList()
)

class NyBehandlingHendelse(
    val morsIdent: String,
    val barnasIdenter: List<String>
)

class RestHenleggBehandlingInfo(
    val årsak: HenleggÅrsak,
    val begrunnelse: String
)

class RestEndreBehandlingstema(
    val behandlingUnderkategori: BehandlingUnderkategori,
    val behandlingKategori: BehandlingKategori
)

enum class HenleggÅrsak(val beskrivelse: String) {
    SØKNAD_TRUKKET("Søknad trukket"),
    FEILAKTIG_OPPRETTET("Behandling feilaktig opprettet"),
    FØDSELSHENDELSE_UGYLDIG_UTFALL("Behandlingen er automatisk henlagt");

    fun tilBehandlingsresultat() = when (this) {
        FEILAKTIG_OPPRETTET -> BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET
        SØKNAD_TRUKKET -> BehandlingResultat.HENLAGT_SØKNAD_TRUKKET
        FØDSELSHENDELSE_UGYLDIG_UTFALL -> BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE
    }
}
