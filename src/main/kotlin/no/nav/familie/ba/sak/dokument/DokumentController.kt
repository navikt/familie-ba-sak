package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class DokumentController(
        private val dokumentService: DokumentService,
        private val vedtakService: VedtakService,
        private val behandlingService: BehandlingService,
        private val fagsakService: FagsakService
) {

    @PostMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun genererVedtaksbrev(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

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

    @PostMapping(path = ["forhaandsvis-brev/{brevMalId}/{behandlingId}"])
    fun hentForhåndsvisning(
            @PathVariable brevMalId: String,
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: GammelManueltBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter brev for mal: $brevMalId")

        if (manueltBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)
        val brevMal = BrevType.values().find { it.malId == brevMalId }
        return if (brevMal != null) {
            dokumentService.genererManueltBrev(behandling, ManueltBrevRequest(
                    mottakerIdent = behandling.fagsak.hentAktivIdent().ident,
                    brevmal = brevMal,
                    fritekst = manueltBrevRequest.fritekst
            )).let {
                Ressurs.success(it)
            }
        } else {
            throw Feil(message = "Finnes ingen støttet brevmal for type $brevMal",
                       frontendFeilmelding = "Klarte ikke hente forhåndsvisning. Finnes ingen støttet brevmal for type $brevMalId")
        }
    }


    @PostMapping(path = ["send-brev/{brevMalId}/{behandlingId}"])
    fun sendBrev(
            @PathVariable brevMalId: String,
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: GammelManueltBrevRequest)
            : Ressurs<RestFagsak> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og send brev: $brevMalId")

        if (manueltBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)
        val brevMal = BrevType.values().find { it.malId == brevMalId }

        return if (brevMal != null) {
            dokumentService.sendManueltBrev(behandling, ManueltBrevRequest(
                    mottakerIdent = behandling.fagsak.hentAktivIdent().ident,
                    brevmal = brevMal,
                    fritekst = manueltBrevRequest.fritekst
            ))
            fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id)
        } else {
            throw Feil(message = "Finnes ingen støttet brevmal for type $brevMal",
                       frontendFeilmelding = "Klarte ikke sende brev. Finnes ingen støttet brevmal for type $brevMalId")
        }
    }

    @PostMapping(path = ["forhaandsvis-brev/{behandlingId}"])
    fun hentForhåndsvisning(
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter brev for mal: ${manueltBrevRequest.brevmal}")

        return dokumentService.genererManueltBrev(behandling = behandlingService.hent(behandlingId),
                                                  manueltBrevRequest = manueltBrevRequest).let {
            Ressurs.success(it)
        }
    }


    @PostMapping(path = ["send-brev/{behandlingId}"])
    fun sendBrev(
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<RestFagsak> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og send brev: ${manueltBrevRequest.brevmal}")

        val behandling = behandlingService.hent(behandlingId)

        dokumentService.sendManueltBrev(behandling = behandlingService.hent(behandlingId),
                                        manueltBrevRequest = manueltBrevRequest)
        return fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id)
    }

    enum class BrevType(val malId: String, val arkivType: String, val visningsTekst: String) {
        INNHENTE_OPPLYSNINGER("innhente-opplysninger", "BARNETRYGD_INNHENTE_OPPLYSNINGER", "innhenting av opplysninger"),
        VEDTAK("vedtak", "BARNETRYGD_VEDTAK", "vedtak");

        override fun toString(): String {
            return visningsTekst
        }
    }

    data class GammelManueltBrevRequest(
            val fritekst: String)

    data class ManueltBrevRequest(
            val brevmal: BrevType,
            val multiselectVerdier: List<String> = emptyList(),
            val mottakerIdent: String,
            val fritekst: String)

    companion object {

        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}
