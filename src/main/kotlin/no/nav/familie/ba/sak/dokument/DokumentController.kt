package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
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
        private val behandlingService: BehandlingService
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
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter brev for mal: $brevMalId")

        if (manueltBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)
        val brevMal = BrevType.values().find { it.malId == brevMalId }
        return if (brevMal != null) {
            dokumentService.genererManueltBrev(behandling, brevMal, manueltBrevRequest).let {
                Ressurs.success(it)
            }
        } else {
            error("Finnes ingen støttet brevmal for type $brevMalId")
        }
    }


    @PostMapping(path = ["send-brev/{brevMalId}/{behandlingId}"])
    fun sendBrev(
            @PathVariable brevMalId: String,
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<String> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og send brev: $brevMalId")

        if (manueltBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)
        val brevMal = BrevType.values().find { it.malId == brevMalId }

        return if (brevMal != null) {
            dokumentService.sendManueltBrev(behandling, brevMal, manueltBrevRequest)
        } else {
            error("Finnes ingen støttet brevmal for type $brevMal")
        }
    }

    enum class BrevType(val malId: String, val arkivType: String) {
        INNHENTE_OPPLYSNINGER("innhente-opplysninger", "BARNETRYGD_INNHENTE_OPPLYSNINGER"),
        VEDTAK("vedtak", "BARNETRYGD_VEDTAK")
    }

    class ManueltBrevRequest(val fritekst: String)

    companion object {
        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}
