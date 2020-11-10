package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.notFound
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/fagsaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val fagsakService: FagsakService,
        private val stegService: StegService,
        private val taskRepository: TaskRepository
) {

    @PostMapping(path = ["/{fagsakId}/utbetaling-begrunnelse"])
    fun leggTilUtbetalingBegrunnelse(@PathVariable fagsakId: Long,
                                     @RequestBody
                                     periode: Periode): ResponseEntity<Ressurs<RestFagsak>> {
        vedtakService.leggTilUtbetalingBegrunnelse(fagsakId = fagsakId,
                                                   periode = periode)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @PutMapping(path = ["/{fagsakId}/utbetaling-begrunnelse/{utbetalingBegrunnelseId}"])
    fun endreUtbetalingBegrunnelse(@PathVariable fagsakId: Long,
                                   @PathVariable utbetalingBegrunnelseId: Long,
                                   @RequestBody
                                   restPutUtbetalingBegrunnelse: RestPutUtbetalingBegrunnelse): ResponseEntity<Ressurs<RestFagsak>> {
        vedtakService.endreUtbetalingBegrunnelse(fagsakId = fagsakId,
                                                 restPutUtbetalingBegrunnelse = restPutUtbetalingBegrunnelse,
                                                 utbetalingBegrunnelseId = utbetalingBegrunnelseId)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @DeleteMapping(path = ["/{fagsakId}/utbetaling-begrunnelse/{utbetalingBegrunnelseId}"])
    fun slettUtbetalingBegrunnelse(@PathVariable fagsakId: Long,
                                   @PathVariable
                                   utbetalingBegrunnelseId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        vedtakService.slettUtbetalingBegrunnelse(fagsakId = fagsakId,
                                                 utbetalingBegrunnelseId = utbetalingBegrunnelseId)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @PostMapping(path = ["/{fagsakId}/send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendBehandlingTilBeslutter(@PathVariable fagsakId: Long,
                                   @RequestParam behandlendeEnhet: String): ResponseEntity<Ressurs<RestFagsak>> {


        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching {
            stegService.håndterSendTilBeslutter(behandling, behandlendeEnhet)
        }.fold(
                onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId)) },
                onFailure = {
                    throw it
                }
        )
    }

    @PostMapping(path = ["/{fagsakId}/iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(@PathVariable fagsakId: Long,
                        @RequestBody restBeslutningPåVedtak: RestBeslutningPåVedtak): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching { stegService.håndterBeslutningForVedtak(behandling, restBeslutningPåVedtak) }
                .fold(
                        onSuccess = {
                            return Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }.fold(
                                    onSuccess = { ResponseEntity.accepted().body(it) },
                                    onFailure = {
                                        illegalState((it.cause?.message ?: it.message).toString(), it)
                                    }
                            )
                        },
                        onFailure = {
                            illegalState((it.cause?.message ?: it.message).toString(), it)
                        }
                )
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}

data class RestBeslutningPåVedtak(
        val beslutning: Beslutning,
        val begrunnelse: String? = null
)

enum class Beslutning {
    GODKJENT,
    UNDERKJENT;

    fun erGodkjent() = this == GODKJENT
}