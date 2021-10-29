package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
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
    private val tilgangService: TilgangService,
    private val brevKlient: BrevKlient,
) {

    @PutMapping("/standardbegrunnelser/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeStandardbegrunnelser(
        @PathVariable
        vedtaksperiodeId: Long,
        @RequestBody
        restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser
    ): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = OPPDATERE_BEGRUNNELSER_HANDLING
        )

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId,
            restPutVedtaksperiodeMedStandardbegrunnelser.standardbegrunnelser
        )

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = vedtak.behandling.fagsak.id))
    }

    @PutMapping("/fritekster/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeMedFritekster(
        @PathVariable
        vedtaksperiodeId: Long,
        @RequestBody
        restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster
    ): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = OPPDATERE_BEGRUNNELSER_HANDLING
        )

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(
            vedtaksperiodeId,
            restPutVedtaksperiodeMedFritekster
        )

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = vedtak.behandling.fagsak.id))
    }

    @GetMapping("/brevbegrunnelser/{vedtaksperiodeId}")
    fun genererBrevBegrunnelserForPeriode(@PathVariable vedtaksperiodeId: Long): ResponseEntity<Ressurs<List<String>>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = OPPDATERE_BEGRUNNELSER_HANDLING
        )

        val begrunnelser = vedtaksperiodeService.genererBrevBegrunnelserForPeriode(vedtaksperiodeId).map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> brevKlient.hentBegrunnelsestekst(it)
                else -> error("Ukjent begrunnelsestype")
            }
        }

        return ResponseEntity.ok(Ressurs.Companion.success(begrunnelser))
    }

    companion object {

        const val OPPDATERE_BEGRUNNELSER_HANDLING = "oppdatere vedtaksperiode med begrunnelser"
    }
}
