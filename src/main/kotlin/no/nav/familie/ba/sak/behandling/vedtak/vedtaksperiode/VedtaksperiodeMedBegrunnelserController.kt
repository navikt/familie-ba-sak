package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
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
}
