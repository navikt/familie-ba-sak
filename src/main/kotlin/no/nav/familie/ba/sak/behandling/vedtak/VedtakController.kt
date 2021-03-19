package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestAvslagBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Avslagsperiode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.RessursUtils.notFound
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
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
        private val tilgangService: TilgangService
) {

    @PostMapping(path = ["/{fagsakId}/vedtak/begrunnelser"])
    fun leggTilVedtakBegrunnelse(@PathVariable fagsakId: Long,
                                 @RequestBody
                                 restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "legge til vedtakbegrunnelser")

        vedtakService.leggTilVedtakBegrunnelse(fagsakId = fagsakId,
                                               restPostVedtakBegrunnelse = restPostVedtakBegrunnelse)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @Deprecated("Bruk slettVedtakBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper")
    @DeleteMapping(path = ["/{fagsakId}/vedtak/begrunnelser/perioder"])
    fun slettVedtakBegrunnelserForPeriode(@PathVariable fagsakId: Long,
                                          @RequestBody
                                          periode: Periode): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "slette vedtakbegrunnelser for periode")

        vedtakService.slettBegrunnelserForPeriode(periode = periode,
                                                  fagsakId = fagsakId)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @DeleteMapping(path = ["/{fagsakId}/vedtak/begrunnelser/perioder-vedtaksbegrunnelsetyper"])
    fun slettVedtakBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(@PathVariable fagsakId: Long,
                                                                   @RequestBody
                                                                   restDeleteVedtakBegrunnelser: RestDeleteVedtakBegrunnelser): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "slette vedtakbegrunnelser for periode")

        vedtakService.slettBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(restDeleteVedtakBegrunnelser = restDeleteVedtakBegrunnelser,
                                                                           fagsakId = fagsakId)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @DeleteMapping(path = ["/{fagsakId}/vedtak/begrunnelser/{vedtakBegrunnelseId}"])
    fun slettVedtakBegrunnelse(@PathVariable fagsakId: Long,
                               @PathVariable
                               vedtakBegrunnelseId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "slette vedtakbegrunnelser")

        vedtakService.slettBegrunnelse(fagsakId = fagsakId,
                                       begrunnelseId = vedtakBegrunnelseId)

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

    @GetMapping(path = ["/{fagsakId}/vedtak/begrunnelser/avslagbegrunnelser"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAvslagBegrunnelser(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<List<RestAvslagBegrunnelser>>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching {
            vedtakService.hentRestAvslagBegrunnelser(behandlingId = behandling.id)
        }.fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(it)) },
                onFailure = { throw it }
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