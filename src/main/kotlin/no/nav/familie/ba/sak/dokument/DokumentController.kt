package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class DokumentController(
        private val dokumentService: DokumentService,
        private val vedtakService: VedtakService,
        private val behandlingService: BehandlingService,
        private val fagsakService: FagsakService,
        private val tilgangService: TilgangService
) {

    @PostMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun genererVedtaksbrev(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} generer vedtaksbrev")
        tilgangService.harTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                             handling = "generere vedtaksbrev")

        val vedtak = vedtakService.hent(vedtakId)

        return dokumentService.genererBrevForVedtak(vedtak).let {
            vedtak.stønadBrevPdF = it
            vedtakService.lagreEllerOppdater(vedtak)
            Ressurs.success(it)
        }
    }

    @GetMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun hentVedtaksbrev(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        val vedtak = vedtakService.hent(vedtakId)

        return dokumentService.hentBrevForVedtak(vedtak)
    }

    @PostMapping(path = ["forhaandsvis-brev/{behandlingId}"])
    fun hentForhåndsvisning(
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter brev for mal: ${manueltBrevRequest.brevmal}")
        return dokumentService.genererManueltBrev(behandling = behandlingService.hent(behandlingId),
                                                  manueltBrevRequest = manueltBrevRequest,
                                                  erForhåndsvisning = true).let { Ressurs.success(it) }
    }


    @PostMapping(path = ["send-brev/{behandlingId}"])
    fun sendBrev(
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<RestFagsak> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og send brev: ${manueltBrevRequest.brevmal}")
        tilgangService.harTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER, handling = "sende brev")

        val behandling = behandlingService.hent(behandlingId)

        dokumentService.sendManueltBrev(behandling = behandlingService.hent(behandlingId),
                                        manueltBrevRequest = manueltBrevRequest)
        return fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id)
    }

    data class ManueltBrevRequest(
            val brevmal: BrevType,
            val multiselectVerdier: List<String> = emptyList(),
            val mottakerIdent: String)

    companion object {

        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}
