package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtaksperioder")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksperiodeMedBegrunnelserController(
        private val fagsakService: FagsakService,
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val tilgangService: TilgangService
) {

    @Deprecated("Fjernes når frontend støtter put på fritekster og standardbegrunnelser")
    @PutMapping("/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeMedBegrunnelser(@PathVariable
                                              vedtaksperiodeId: Long,
                                              @RequestBody
                                              restPutVedtaksperiodeMedBegrunnelse: RestPutVedtaksperiodeMedBegrunnelse): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "oppdatere vedtaksperiode med begrunnelser")

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
                vedtaksperiodeId,
                restPutVedtaksperiodeMedBegrunnelse
        )

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = vedtak.behandling.fagsak.id))
    }

    @PutMapping("/standardbegrunnelser/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeStandardbegrunnelser(@PathVariable
                                                   vedtaksperiodeId: Long,
                                                   @RequestBody
                                                   restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "oppdatere vedtaksperiode med begrunnelser")

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
                vedtaksperiodeId,
                restPutVedtaksperiodeMedStandardbegrunnelser
        )

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = vedtak.behandling.fagsak.id))
    }

    @PutMapping("/fritekster/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeMedFritekster(@PathVariable
                                            vedtaksperiodeId: Long,
                                            @RequestBody
                                            restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "oppdatere vedtaksperiode med begrunnelser")

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(
                vedtaksperiodeId,
                restPutVedtaksperiodeMedFritekster
        )

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = vedtak.behandling.fagsak.id))
    }
}
