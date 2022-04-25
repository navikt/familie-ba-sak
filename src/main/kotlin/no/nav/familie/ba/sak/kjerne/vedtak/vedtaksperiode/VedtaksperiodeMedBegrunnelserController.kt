package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.ekstern.restDomene.RestGenererVedtaksperioderForOverstyrtEndringstidspunkt
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutGenererFortsattInnvilgetVedtaksperioder
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.BrevPeriodeService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ExceptionHandler
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
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakService: VedtakService,
    private val tilgangService: TilgangService,
    private val brevKlient: BrevKlient,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val brevPeriodeService: BrevPeriodeService,
) {
    @ExceptionHandler(FantIkkeVedtaksperiodeFeil::class)
    fun handleFantIkkeVedtaksperiodeFeil(feil: FantIkkeVedtaksperiodeFeil): ResponseEntity<Ressurs<Nothing>> {

        return RessursUtils.funksjonellFeil(
            FunksjonellFeil(
                melding = feil.message!!,
                frontendFeilmelding = feil.frontendFeilmelding
            )
        )
    }

    @PutMapping("/standardbegrunnelser/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeStandardbegrunnelser(
        @PathVariable
        vedtaksperiodeId: Long,
        @RequestBody
        restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = OPPDATERE_BEGRUNNELSER_HANDLING
        )

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId,
            restPutVedtaksperiodeMedStandardbegrunnelser.standardbegrunnelser
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = vedtak.behandling.id)))
    }

    @PutMapping("/fritekster/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeMedFritekster(
        @PathVariable
        vedtaksperiodeId: Long,
        @RequestBody
        restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = OPPDATERE_BEGRUNNELSER_HANDLING
        )

        val vedtak = vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(
            vedtaksperiodeId,
            restPutVedtaksperiodeMedFritekster
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = vedtak.behandling.id)))
    }

    @PutMapping("/endringstidspunkt")
    fun genererVedtaksperioderTilOgMedFørsteEndringstidspunkt(
        @RequestBody restGenererVedtaksperioder: RestGenererVedtaksperioderForOverstyrtEndringstidspunkt
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        vedtaksperiodeService.genererVedtaksperiodeForOverstyrtEndringstidspunkt(restGenererVedtaksperioder)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = restGenererVedtaksperioder.behandlingId)
            )
        )
    }

    @GetMapping("/brevbegrunnelser/{vedtaksperiodeId}")
    fun genererBrevBegrunnelserForPeriode(@PathVariable vedtaksperiodeId: Long): ResponseEntity<Ressurs<List<String>>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente genererte begrunnelser"
        )

        val begrunnelser = brevPeriodeService.genererBrevBegrunnelserForPeriode(vedtaksperiodeId).map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> brevKlient.hentBegrunnelsestekst(it)
                else -> throw Feil("Ukjent begrunnelsestype")
            }
        }

        return ResponseEntity.ok(Ressurs.Companion.success(begrunnelser))
    }

    /*
    * Dette endepunktet brukes for å overstyre hva slags vedtaksperioder man ønsker når resultatet er fortsatt innvilget.
    * Muligheter:
    * - skalGenererePerioderForFortsattInnvilget = false -> det blir kun generert 1 periode, uten dato (default valg for fortsatt innvilget)
    * - skalGenererePerioderForFortsattInnvilget = true -> det blir generert 'vanlige' perioder (overstyrer default for fortsatt innvilget)
    */
    @PutMapping("/overstyr-fortsatt-innvilget-vedtaksperioder")
    fun genererFortsattInnvilgetVedtaksperioder(
        @RequestBody restPutGenererFortsattInnvilgetVedtaksperioder: RestPutGenererFortsattInnvilgetVedtaksperioder
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "oppdater vedtaksperioder fortsatt innvilget"
        )
        val vedtak =
            vedtakService.hentAktivForBehandlingThrows(behandlingId = restPutGenererFortsattInnvilgetVedtaksperioder.behandlingId)
        if (vedtak.behandling.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
            throw FunksjonellFeil(
                melding = "Kan ikke overstyre vedtaksperioder når resultatet ikke er fortsatt innvilget."
            )
        }
        vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(
            vedtak = vedtak,
            skalOverstyreFortsattInnvilget = restPutGenererFortsattInnvilgetVedtaksperioder.skalGenererePerioderForFortsattInnvilget
        )
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = vedtak.behandling.id)))
    }

    companion object {
        const val OPPDATERE_BEGRUNNELSER_HANDLING = "oppdatere vedtaksperiode med begrunnelser"
    }
}
