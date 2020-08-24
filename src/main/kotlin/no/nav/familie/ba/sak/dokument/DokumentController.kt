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

    @PostMapping(path = ["generer-brev/{brevMal}/{behandlingId}"])
    fun genererBrev(
            @PathVariable brevMal: String,
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer brev: $brevMal")

        if (manueltBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)

        return if (ManueltBrev.values().any { it.malId == brevMal }) {
            dokumentService.genererBrevForInnhenteOpplysninger(behandling, manueltBrevRequest).let {
                Ressurs.success(it)
            }
        } else {
            error("Finnes ingen støttet brevmal for type $brevMal")
        }
    }

    @PostMapping(path = ["generer-brev/{brevMalId}/{behandlingId}"])
    fun genererBrev2(
            @PathVariable brevMalId: String,
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer brev: $brevMalId")

        if (manueltBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)
        val brevMal = ManueltBrev.values().find { it.malId == brevMalId }
        return if (brevMal !== null) {
            dokumentService.genererManueltBrev(behandling, brevMal, manueltBrevRequest).let {
                Ressurs.success(it)
            }
        } else {
            error("Finnes ingen støttet brevmal for type $brevMalId")
        }
    }

    @PostMapping(path = ["generer-og-send-brev/{brevMalId}/{behandlingId}"])
    fun genererOgSendBrev(
            @PathVariable brevMalId: String,
            @PathVariable behandlingId: Long,
            @RequestBody manueltBrevRequest: ManueltBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} genererer og send brev: $brevMalId")

        if (manueltBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)
        val brevMal = ManueltBrev.values().find { it.malId == brevMalId }

        return if (brevMal != null) {
            dokumentService.genererOgSendManueltBrev(behandling, brevMal, manueltBrevRequest).let {
                Ressurs.success(it)
            }
        } else {
            error("Finnes ingen støttet brevmal for type $brevMal")
        }
    }

    enum class ManueltBrev(val malId: String) {
        INNHENTE_OPPLYSNINGER("innhente-opplysninger")
    }

    class ManueltBrevRequest(val fritekst: String)

    companion object {
        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}
