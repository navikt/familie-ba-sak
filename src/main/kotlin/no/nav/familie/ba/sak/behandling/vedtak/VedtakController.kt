package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
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

    @PostMapping(path = ["/{fagsakId}/vedtak/fritekster"])
    fun settFritekstVedtakBegrunnelserPåVedtaksperiodeOgType(@PathVariable fagsakId: Long,
                                                             @RequestBody
                                                             restPostFritekstVedtakBegrunnelser: RestPostFritekstVedtakBegrunnelser): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "legge til vedtakbegrunnelser")

        vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(fagsakId = fagsakId,
                                                                     restPostFritekstVedtakBegrunnelser = restPostFritekstVedtakBegrunnelser)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }


    @PostMapping(path = ["/{fagsakId}/vedtak/begrunnelser"])
    fun leggTilVedtakBegrunnelse(@PathVariable fagsakId: Long,
                                 @RequestBody
                                 restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "legge til vedtakbegrunnelser")

        vedtakService.leggTilBegrunnelse(fagsakId = fagsakId,
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