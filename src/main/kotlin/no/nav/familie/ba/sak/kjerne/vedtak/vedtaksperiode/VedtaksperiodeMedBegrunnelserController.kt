package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.BrevPeriodeService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
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
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val tilgangService: TilgangService,
    private val brevKlient: BrevKlient,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val brevPeriodeService: BrevPeriodeService,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
) {

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

    @GetMapping("/brevbegrunnelser/{vedtaksperiodeId}")
    fun genererBrevBegrunnelserForPeriode(@PathVariable vedtaksperiodeId: Long): ResponseEntity<Ressurs<List<String>>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente genererte begrunnelser"
        )

        val begrunnelser = brevPeriodeService.genererBrevBegrunnelserForPeriode(vedtaksperiodeId).map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> try {
                    brevKlient.hentBegrunnelsestekst(it)
                } catch (e: Exception) {
                    val feilmelding = hentFeilmeldingMedBrevperiodeData(vedtaksperiodeId, e)
                    when (e) {
                        is FunksjonellFeil -> throw FunksjonellFeil(melding = feilmelding, throwable = e)
                        else -> throw Feil(message = feilmelding, throwable = e)
                    }
                }
                else -> throw Feil("Ukjent begrunnelsestype")
            }
        }

        return ResponseEntity.ok(Ressurs.Companion.success(begrunnelser))
    }

    private fun hentFeilmeldingMedBrevperiodeData(vedtaksperiodeId: Long, e: Exception): String =
        "Kunne ikke hente brevbegrunnelser for vedtaksperiode " +
            "på behandling ${vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId).vedtak.behandling}. " +
            "Feilmelding: ${e.message}" +
            "Data for brevperiode var" +
            brevPeriodeService
                .hentBrevperiodeData(vedtaksperiodeId)
                .tilBrevperiodeForLogging()
                .convertDataClassToJson()

    companion object {

        const val OPPDATERE_BEGRUNNELSER_HANDLING = "oppdatere vedtaksperiode med begrunnelser"
    }
}
