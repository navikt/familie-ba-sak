package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.byggMottakerdataFraBehandling
import no.nav.familie.ba.sak.kjerne.brev.domene.byggMottakerdataFraFagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokdistkanal.Distribusjonskanal
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
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
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val dokumentService: DokumentService,
    private val dokumentGenereringService: DokumentGenereringService,
    private val vedtakService: VedtakService,
    private val tilgangService: TilgangService,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val dokumentDistribueringService: DokumentDistribueringService,
    private val pdlRestClient: PdlRestClient,
) {
    @PostMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun genererVedtaksbrev(
        @PathVariable vedtakId: Long,
    ): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} generer vedtaksbrev")
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "generere vedtaksbrev",
        )

        val vedtak = vedtakService.hent(vedtakId)
        tilgangService.validerTilgangTilBehandling(behandlingId = vedtak.behandling.id, event = AuditLoggerEvent.UPDATE)

        return dokumentGenereringService.genererBrevForVedtak(vedtak).let {
            vedtak.stønadBrevPdF = it
            vedtakService.oppdater(vedtak)
            Ressurs.success(it)
        }
    }

    @GetMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun hentVedtaksbrev(
        @PathVariable vedtakId: Long,
    ): Ressurs<ByteArray> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente vedtaksbrev",
        )

        val vedtak = vedtakService.hent(vedtakId)

        tilgangService.validerTilgangTilBehandling(behandlingId = vedtak.behandling.id, event = AuditLoggerEvent.ACCESS)

        return dokumentService.hentBrevForVedtak(vedtak)
    }

    @PostMapping(path = ["forhaandsvis-brev/{behandlingId}"])
    fun hentForhåndsvisning(
        @PathVariable behandlingId: Long,
        @RequestBody manueltBrevRequest: ManueltBrevRequest,
    ): Ressurs<ByteArray> {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} henter forhåndsvisning av brev " +
                "for mal: ${manueltBrevRequest.brevmal}",
        )
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "hente forhåndsvisning brev",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        return dokumentGenereringService
            .genererManueltBrev(
                manueltBrevRequest =
                    manueltBrevRequest.byggMottakerdataFraBehandling(
                        behandling,
                        persongrunnlagService,
                        arbeidsfordelingService,
                    ),
                erForhåndsvisning = true,
                fagsak = behandling.fagsak,
            ).let { Ressurs.success(it) }
    }

    @PostMapping(path = ["send-brev/{behandlingId}"])
    fun sendBrev(
        @PathVariable behandlingId: Long,
        @RequestBody manueltBrevRequest: ManueltBrevRequest,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og sender brev: ${manueltBrevRequest.brevmal}")
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "sende brev",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        dokumentService.sendManueltBrev(
            manueltBrevRequest =
                manueltBrevRequest.byggMottakerdataFraBehandling(
                    behandling,
                    persongrunnlagService,
                    arbeidsfordelingService,
                ),
            behandling = behandling,
            fagsakId = behandling.fagsak.id,
        )
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandlingId),
            ),
        )
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/forhaandsvis-brev"])
    fun hentForhåndsvisningPåFagsak(
        @PathVariable fagsakId: Long,
        @RequestBody manueltBrevRequest: ManueltBrevRequest,
    ): Ressurs<ByteArray> {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} henter forhåndsvisning av brev på fagsak $fagsakId " +
                "for mal: ${manueltBrevRequest.brevmal}",
        )
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "hente forhåndsvisning brev",
        )

        val fagsak = fagsakService.hentPåFagsakId(fagsakId)

        val oppdatertManueltBrevRequest = manueltBrevRequest.byggMottakerdataFraFagsak(fagsak, arbeidsfordelingService, pdlRestClient)

        return dokumentGenereringService
            .genererManueltBrev(
                manueltBrevRequest = oppdatertManueltBrevRequest,
                erForhåndsvisning = true,
                fagsak = fagsak,
            ).let { Ressurs.success(it) }
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/send-brev"])
    fun sendBrevPåFagsak(
        @PathVariable fagsakId: Long,
        @RequestBody manueltBrevRequest: ManueltBrevRequest,
    ): ResponseEntity<Ressurs<RestMinimalFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og sender brev på fagsak $fagsakId: ${manueltBrevRequest.brevmal}")
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "sende brev",
        )

        val fagsak = fagsakService.hentPåFagsakId(fagsakId)
        val oppdatertManueltBrevRequest = manueltBrevRequest.byggMottakerdataFraFagsak(fagsak, arbeidsfordelingService, pdlRestClient)

        dokumentService.sendManueltBrev(
            manueltBrevRequest = oppdatertManueltBrevRequest,
            fagsakId = fagsakId,
        )
        return ResponseEntity.ok(Ressurs.success(fagsakService.lagRestMinimalFagsak(fagsakId = fagsakId)))
    }

    @PostMapping(path = ["/distribusjonskanal"])
    fun hentDistribusjonskanal(
        @RequestBody personIdent: PersonIdent,
    ): Ressurs<Distribusjonskanal> = Ressurs.success(dokumentDistribueringService.hentDistribusjonskanal(personIdent))

    companion object {
        private val logger = LoggerFactory.getLogger(DokumentController::class.java)
    }
}
