package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.ok
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(private val fagsakService: FagsakService,
                           private val stegService: StegService,
                           private val behandlingsService: BehandlingService,
                           private val taskRepository: TaskRepository) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping(path = ["behandlinger"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        if (nyBehandling.søkersIdent.isBlank()) {
            throw Feil(message = "Søkers ident kan ikke være blank",
                       frontendFeilmelding = "Klarte ikke å opprette behandling. Mangler ident på bruker.")
        }

        if (nyBehandling.behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD && nyBehandling.barnasIdenter.isEmpty()) {
            throw Feil(message = "Listen med barn er tom ved opprettelse av migreringsbehandling",
                       frontendFeilmelding = "Klarte ikke å opprette behandling. Mangler barna det gjelder.")
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

    @PutMapping(path = ["behandlinger"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettEllerOppdaterBehandlingFraHendelse(@RequestBody
                                                  nyBehandling: NyBehandlingHendelse): ResponseEntity<Ressurs<String>> {
        return try {
            val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandling))
            taskRepository.save(task)
            ok("Task opprettet for behandling av fødselshendelse.")
        } catch (ex: Throwable) {
            illegalState("Task kunne ikke opprettes for behandling av fødselshendelse: ${ex.message}", ex)
        }
    }

    @PutMapping(path = ["behandlinger/{behandlingId}/henlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun henleggBehandling(@PathVariable(name = "behandlingId") behandlingId: Long,
                          @RequestBody henleggInfo: RestHenleggBehandlingInfo): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingsService.hent(behandlingId)
        val response = stegService.håndterHenleggBehandling(behandling, henleggInfo)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = response.fagsak.id))
    }
}

data class NyBehandling(
        val kategori: BehandlingKategori,
        val underkategori: BehandlingUnderkategori,
        val søkersIdent: String,
        val behandlingType: BehandlingType,
        val journalpostID: String? = null,
        val behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
        val skalBehandlesAutomatisk: Boolean = false,
        val barnasIdenter: List<String> = emptyList())

class NyBehandlingHendelse(
        val morsIdent: String,
        val barnasIdenter: List<String>
)

class RestHenleggBehandlingInfo(
        val årsak: HenleggÅrsak,
        val begrunnelse: String
)

enum class HenleggÅrsak() {
    SØKNAD_TRUKKET,
    FEILAKTIG_OPPRETTET
}