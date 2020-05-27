package no.nav.familie.ba.sak.behandling

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.kontrakter.felles.Ressurs
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
                           private val stegService: StegService) {

    private val antallManuelleBehandlingerOpprettet: Map<BehandlingType, Counter> = initBehandlingMetrikker("manuell")

    private val antallAutomatiskeBehandlingerOpprettet: Map<BehandlingType, Counter> = initBehandlingMetrikker("automatisk")

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping(path = ["behandlinger"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        if (nyBehandling.søkersIdent.isBlank()) {
            throw Feil(message = "Søkers ident kan ikke være blank",
                       frontendFeilmelding = "Klarte ikke å opprette behandling. Mangler ident på bruker.")
        }

        return Result.runCatching {
            stegService.håndterNyBehandling(nyBehandling)
        }.fold(
                onSuccess = {
                    val restFagsak = ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.fagsak.id))
                    antallManuelleBehandlingerOpprettet[nyBehandling.behandlingType]?.increment()
                    return restFagsak
                },
                onFailure = {
                    throw it
                }
        )
    }

    @PutMapping(path = ["behandlinger"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettEllerOppdaterBehandlingFraHendelse(@RequestBody
                                                  nyBehandling: NyBehandlingHendelse): ResponseEntity<Ressurs<RestFagsak>> {
        return Result.runCatching { stegService.håndterNyBehandlingFraHendelse(nyBehandling) }
                .fold(
                        onFailure = {
                            illegalState("Opprettelse av behandling fra hendelse feilet: ${it.message}", it)
                        },
                        onSuccess = {
                            val restFagsak = ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.fagsak.id))
                            antallAutomatiskeBehandlingerOpprettet[BehandlingType.FØRSTEGANGSBEHANDLING]?.increment()
                            return restFagsak
                        }
                )
    }

    private fun initBehandlingMetrikker(type: String): Map<BehandlingType, Counter> {
        return BehandlingType.values().map {
            it to Metrics.counter("behandling.opprettet.$type", "type",
                                  it.name,
                                  "beskrivelse",
                                  it.visningsnavn)
        }.toMap()
    }
}

class NyBehandling(
        val kategori: BehandlingKategori,
        val underkategori: BehandlingUnderkategori,
        val søkersIdent: String,
        val behandlingType: BehandlingType,
        val journalpostID: String? = null)

class NyBehandlingHendelse(
        val søkersIdent: String,
        val barnasIdenter: List<String>
)