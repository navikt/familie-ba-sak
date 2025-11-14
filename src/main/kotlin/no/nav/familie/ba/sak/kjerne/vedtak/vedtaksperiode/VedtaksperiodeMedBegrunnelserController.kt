package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestGenererVedtaksperioderForOverstyrtEndringstidspunkt
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.BrevBegrunnelseFeil
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.hentBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.finnBegrunnelseGrunnlagPerPerson
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
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val kodeverkService: KodeverkService,
    private val testVerktøyService: TestVerktøyService,
) {
    @PutMapping("/standardbegrunnelser/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeStandardbegrunnelser(
        @PathVariable
        vedtaksperiodeId: Long,
        @RequestBody
        restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser,
    ): ResponseEntity<Ressurs<List<RestUtvidetVedtaksperiodeMedBegrunnelser>>> {
        val behandlingId =
            vedtaksperiodeHentOgPersisterService.finnBehandlingIdFor(vedtaksperiodeId)
                ?: throw FunksjonellFeil("Vedtaksperiodene har blitt oppdatert. Oppdater siden og forsøk å legge til begrunnelser på nytt.")
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = OPPDATERE_BEGRUNNELSER_HANDLING,
        )

        val standardbegrunnelser =
            restPutVedtaksperiodeMedStandardbegrunnelser.standardbegrunnelser.map {
                IVedtakBegrunnelse.konverterTilEnumVerdi(it)
            }

        val nasjonalebegrunnelser = standardbegrunnelser.filterIsInstance<Standardbegrunnelse>()
        val eøsStandardbegrunnelser = standardbegrunnelser.filterIsInstance<EØSStandardbegrunnelse>()

        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiodeId,
            standardbegrunnelserFraFrontend = nasjonalebegrunnelser,
            eøsStandardbegrunnelserFraFrontend = eøsStandardbegrunnelser,
        )

        return ResponseEntity.ok(
            Ressurs.success(
                vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
                    behandlingId,
                ),
            ),
        )
    }

    @PutMapping("/fritekster/{vedtaksperiodeId}")
    fun oppdaterVedtaksperiodeMedFritekster(
        @PathVariable
        vedtaksperiodeId: Long,
        @RequestBody
        restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster,
    ): ResponseEntity<Ressurs<List<RestUtvidetVedtaksperiodeMedBegrunnelser>>> {
        val behandlingId = vedtaksperiodeHentOgPersisterService.finnBehandlingIdFor(vedtaksperiodeId) ?: throw FunksjonellFeil("Vedtaksperiodene har blitt oppdatert. Oppdater siden og forsøk å legge til fritekst på nytt.")
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = OPPDATERE_BEGRUNNELSER_HANDLING,
        )

        vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(
            vedtaksperiodeId,
            restPutVedtaksperiodeMedFritekster,
        )

        return ResponseEntity.ok(
            Ressurs.success(
                vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
                    behandlingId,
                ),
            ),
        )
    }

    @PutMapping("/endringstidspunkt")
    fun genererVedtaksperioderTilOgMedFørsteEndringstidspunkt(
        @RequestBody restGenererVedtaksperioder: RestGenererVedtaksperioderForOverstyrtEndringstidspunkt,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandlingId = restGenererVedtaksperioder.behandlingId
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdaterer vedtaksperiode med endringstidspunkt",
        )

        vedtaksperiodeService.oppdaterEndringstidspunktOgGenererVedtaksperioderPåNytt(restGenererVedtaksperioder)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandlingId),
            ),
        )
    }

    @GetMapping("/brevbegrunnelser/{vedtaksperiodeId}")
    fun genererBrevBegrunnelserForPeriode(
        @PathVariable vedtaksperiodeId: Long,
    ): ResponseEntity<Ressurs<Set<String>>> {
        val behandlingId =
            vedtaksperiodeHentOgPersisterService.finnBehandlingIdFor(vedtaksperiodeId)
                ?: throw FunksjonellFeil("Vedtaksperiodene har blitt oppdatert. Oppdater siden og forsøk å legge til begrunnelser på nytt.")

        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente genererte begrunnelser",
        )

        val vedtaksperiode = vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val grunnlagForBegrunnelser = vedtaksperiodeService.hentGrunnlagForBegrunnelse(behandlingId)
        val begrunnelsesGrunnlagPerPerson = vedtaksperiode.finnBegrunnelseGrunnlagPerPerson(grunnlagForBegrunnelser)

        val brevBegrunnelser =
            try {
                vedtaksperiode.hentBegrunnelser(
                    grunnlagForBegrunnelse = grunnlagForBegrunnelser,
                    begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
                    landkoder = kodeverkService.hentLandkoderISO2(),
                )
            } catch (e: BrevBegrunnelseFeil) {
                secureLogger.info(
                    "Brevbegrunnelsefeil for behandling $behandlingId, " +
                        "fagsak ${vedtaksperiode.vedtak.behandling.fagsak.id} " +
                        "på periode ${vedtaksperiode.fom} - ${vedtaksperiode.tom}. " +
                        "\nAutogenerert test:\n" + testVerktøyService.hentBegrunnelsetest(behandlingId),
                )
                throw e
            }

        val begrunnelser =
            brevBegrunnelser.map {
                when (it) {
                    is FritekstBegrunnelse -> it.fritekst
                    is BegrunnelseMedData -> brevKlient.hentBegrunnelsestekst(it, vedtaksperiode)
                }
            }

        return ResponseEntity.ok(Ressurs.Companion.success(begrunnelser.toSet()))
    }

    @GetMapping(path = ["/behandling/{behandlingId}/hent-vedtaksperioder"])
    fun hentRestUtvidetVedtaksperiodeMedBegrunnelser(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<RestUtvidetVedtaksperiodeMedBegrunnelser>>> =
        ResponseEntity.ok(
            Ressurs.success(
                vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(behandlingId),
            ),
        )

    companion object {
        const val OPPDATERE_BEGRUNNELSER_HANDLING = "oppdatere vedtaksperiode med begrunnelser"
    }
}
