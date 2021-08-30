package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.dokument.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.dokument.domene.byggMottakerdata
import no.nav.familie.ba.sak.kjerne.dokument.domene.leggTilEnhet
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.sikkerhet.validering.VedtaktilgangConstraint
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
        private val tilgangService: TilgangService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService
) {

    @PostMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun genererVedtaksbrev(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} generer vedtaksbrev")
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "generere vedtaksbrev")

        val vedtak = vedtakService.hent(vedtakId)

        return dokumentService.genererBrevForVedtak(vedtak).let {
            vedtak.stønadBrevPdF = it
            vedtakService.oppdater(vedtak)
            Ressurs.success(it)
        }
    }

    @GetMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun hentVedtaksbrev(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                                                      handling = "hente vedtaksbrev")

        val vedtak = vedtakService.hent(vedtakId)

        return dokumentService.hentBrevForVedtak(vedtak)
    }

    @PostMapping(path = ["forhaandsvis-brev/{behandlingId}"])
    fun hentForhåndsvisning(
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter forhåndsvisning av brev for mal: ${manueltBrevRequest.brevmal}")
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "hente forhåndsvisning brev")

        val behandling = behandlingService.hent(behandlingId)

        return dokumentService.genererManueltBrev(manueltBrevRequest = manueltBrevRequest.byggMottakerdata(behandling,
                                                                                                           persongrunnlagService,
                                                                                                           arbeidsfordelingService),
                                                  erForhåndsvisning = true).let { Ressurs.success(it) }
    }


    @PostMapping(path = ["send-brev/{behandlingId}"])
    fun sendBrev(
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<RestFagsak> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og sender brev: ${manueltBrevRequest.brevmal}")
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "sende brev")

        val behandling = behandlingService.hent(behandlingId)

        dokumentService.sendManueltBrev(manueltBrevRequest = manueltBrevRequest.byggMottakerdata(behandling,
                                                                                                 persongrunnlagService,
                                                                                                 arbeidsfordelingService),
                                        behandling = behandling,
                                        fagsakId = behandling.fagsak.id)
        return fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id)
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/forhaandsvis-brev"])
    fun hentForhåndsvisningPåFagsak(
            @PathVariable fagsakId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter forhåndsvisning av brev på fagsak $fagsakId for mal: ${manueltBrevRequest.brevmal}")
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "hente forhåndsvisning brev")

        return dokumentService.genererManueltBrev(manueltBrevRequest = manueltBrevRequest.leggTilEnhet(arbeidsfordelingService),
                                                  erForhåndsvisning = true).let { Ressurs.success(it) }
    }


    @PostMapping(path = ["/fagsak/{fagsakId}/send-brev"])
    fun sendBrevPåFagsak(
            @PathVariable fagsakId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<RestFagsak> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og sender brev på fagsak $fagsakId: ${manueltBrevRequest.brevmal}")
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "sende brev")

        dokumentService.sendManueltBrev(manueltBrevRequest = manueltBrevRequest.leggTilEnhet(arbeidsfordelingService),
                                        fagsakId = fagsakId)
        return fagsakService.hentRestFagsak(fagsakId = fagsakId)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DokumentController::class.java)
    }
}
