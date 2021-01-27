package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.RessursUtils.notFound
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fagsaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val fagsakService: FagsakService,
        private val stegService: StegService,
        private val tilgangService: TilgangService
) {

    @PostMapping(path = ["/{fagsakId}/utbetaling-begrunnelse"])
    fun leggTilUtbetalingBegrunnelse(@PathVariable fagsakId: Long,
                                     @RequestBody
                                     periode: Periode): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.harTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                             handling = "legge til utbetalingsbegrunnelse")

        vedtakService.leggTilUtbetalingBegrunnelse(fagsakId = fagsakId,
                                                   periode = periode)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @PutMapping(path = ["/{fagsakId}/utbetaling-begrunnelse/{utbetalingBegrunnelseId}"])
    fun endreUtbetalingBegrunnelse(@PathVariable fagsakId: Long,
                                   @PathVariable utbetalingBegrunnelseId: Long,
                                   @RequestBody
                                   restPutUtbetalingBegrunnelse: RestPutUtbetalingBegrunnelse): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.harTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                             handling = "endre utbetalingsbegrunnelse")

        vedtakService.endreUtbetalingBegrunnelse(fagsakId = fagsakId,
                                                 restPutUtbetalingBegrunnelse = restPutUtbetalingBegrunnelse,
                                                 utbetalingBegrunnelseId = utbetalingBegrunnelseId)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @DeleteMapping(path = ["/{fagsakId}/utbetaling-begrunnelse/{utbetalingBegrunnelseId}"])
    fun slettUtbetalingBegrunnelse(@PathVariable fagsakId: Long,
                                   @PathVariable
                                   utbetalingBegrunnelseId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.harTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                             handling = "slette utbetalingsbegrunnelse")

        vedtakService.slettUtbetalingBegrunnelse(fagsakId = fagsakId,
                                                 utbetalingBegrunnelseId = utbetalingBegrunnelseId)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @PostMapping(path = ["/{fagsakId}/send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendBehandlingTilBeslutter(@PathVariable fagsakId: Long,
                                   @RequestParam behandlendeEnhet: String): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        stegService.håndterSendTilBeslutter(behandling, behandlendeEnhet)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @PostMapping(path = ["/{fagsakId}/iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(@PathVariable fagsakId: Long,
                        @RequestBody restBeslutningPåVedtak: RestBeslutningPåVedtak): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        stegService.håndterBeslutningForVedtak(behandling, restBeslutningPåVedtak)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
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