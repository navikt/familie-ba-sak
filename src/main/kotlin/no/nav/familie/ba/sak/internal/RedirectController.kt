package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping(value = ["/redirect"])
class RedirectController(
    private val envService: EnvService,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val oppgaveService: OppgaveService,
) {
    @GetMapping("/behandling/{behandlingId}")
    fun redirectTilBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Any> {
        val hostname = hentHostname()
        val behandling = behandlingRepository.finnBehandlingNullable(behandlingId)
        return if (behandling == null) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fant ikke behandling med id $behandlingId")
        } else {
            ResponseEntity
                .status(302)
                .location(URI.create("$hostname/fagsak/${behandling.fagsak.id}/$behandlingId/"))
                .build()
        }
    }

    @GetMapping("/fagsak/{fagsakId}")
    fun redirectTilFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Any> {
        val hostname = hentHostname()
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
        return if (fagsak == null) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fant ikke fagsak med id $fagsakId")
        } else {
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("$hostname/fagsak/$fagsakId/"))
                .build()
        }
    }

    @GetMapping("/oppgave/{gsakId}")
    fun redirectTilBehandlingFraOppgaveId(
        @PathVariable gsakId: String,
    ): ResponseEntity<Any> {
        val hostname = hentHostname()
        val behandling = oppgaveService.hentBehandlingForOppgave(gsakId)
        return if (behandling == null) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fant ikke behandling knyttet til oppgave med id $gsakId")
        } else {
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("$hostname/fagsak/${behandling.fagsak.id}/${behandling.id}"))
                .build()
        }
    }

    private fun hentHostname(): String =
        when {
            envService.erDev() -> "http://localhost:8000"
            envService.erPreprod() -> "https://barnetrygd.intern.dev.nav.no"
            envService.erProd() -> "https://barnetrygd.intern.nav.no"
            else -> throw Feil("Klarer ikke å utlede miljø for redirect til fagsak")
        }
}
