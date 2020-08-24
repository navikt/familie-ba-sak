package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestStønadBrevBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.RessursUtils.forbidden
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.notFound
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.OpphørVedtakTask
import no.nav.familie.ba.sak.validering.FagsaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/fagsaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val fagsakService: FagsakService,
        private val stegService: StegService,
        private val taskRepository: TaskRepository
) {

    @PostMapping(path = ["/{fagsakId}/legg-til-stønad-brev-begrunnelse"])
    fun leggTilStønadBrevBegrunnelse(@PathVariable fagsakId: Long,
                                     @RequestBody
                                     restStønadBrevBegrunnelse: RestStønadBrevBegrunnelse): ResponseEntity<Ressurs<List<RestStønadBrevBegrunnelse>>> {
        var nyStønadBrevBegrunnelser = vedtakService.leggTilStønadBrevBegrunnelse(fagsakId = fagsakId, restStønadBrevBegrunnelse = restStønadBrevBegrunnelse)

        return ResponseEntity.ok(Ressurs.success(nyStønadBrevBegrunnelser))
    }

    @PutMapping(path = ["/{fagsakId}/endre-stønad-brev-begrunnelse"])
    fun endreStønadBrevBegrunnelse(@PathVariable fagsakId: Long,
                                   @RequestBody
                                   restStønadBrevBegrunnelse: RestStønadBrevBegrunnelse): ResponseEntity.BodyBuilder {
        return ResponseEntity.ok()
    }

    @PostMapping(path = ["/{fagsakId}/send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendBehandlingTilBeslutter(@PathVariable fagsakId: Long,
                                   @RequestParam behandlendeEnhet: String): ResponseEntity<Ressurs<RestFagsak>> {


        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching {
            stegService.håndterSendTilBeslutter(behandling, behandlendeEnhet)
        }.fold(
                onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId)) },
                onFailure = {
                    throw it
                }
        )
    }

    @PostMapping(path = ["/{fagsakId}/iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(@PathVariable fagsakId: Long,
                        @RequestBody restBeslutningPåVedtak: RestBeslutningPåVedtak): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching { stegService.håndterBeslutningForVedtak(behandling, restBeslutningPåVedtak) }
                .fold(
                        onSuccess = {
                            return Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }.fold(
                                    onSuccess = { ResponseEntity.accepted().body(it) },
                                    onFailure = {
                                        illegalState((it.cause?.message ?: it.message).toString(), it)
                                    }
                            )
                        },
                        onFailure = {
                            illegalState((it.cause?.message ?: it.message).toString(), it)
                        }
                )
    }

    @PostMapping(path = ["/{fagsakId}/opphoer-migrert-vedtak/v2"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opphørMigrertVedtak(@PathVariable @FagsaktilgangConstraint fagsakId: Long, @RequestBody
    opphørsvedtak: Opphørsvedtak): ResponseEntity<Ressurs<String>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter task for opphør av migrert vedtak for fagsak med id $fagsakId")

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                     ?: return notFound("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        if (behandling.status != BehandlingStatus.IVERKSATT && behandling.status != BehandlingStatus.FERDIGSTILT) {
            return forbidden("Prøver å opphøre et vedtak for behandling ${behandling.id}, som ikke er iverksatt/ferdigstilt")
        }

        val task = OpphørVedtakTask.opprettOpphørVedtakTask(behandling,
                                                            vedtak,
                                                            saksbehandlerId,
                                                            if (behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD)
                                                                BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
                                                            else BehandlingType.TEKNISK_OPPHØR,
                                                            opphørsvedtak.opphørsdato)
        taskRepository.save(task)

        return ResponseEntity.ok(Ressurs.success("Task for opphør av migrert behandling og vedtak på fagsak $fagsakId opprettet"))
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}

data class Opphørsvedtak(
        val opphørsdato: LocalDate
)

data class RestBeslutningPåVedtak(
        val beslutning: Beslutning,
        val begrunnelse: String? = null
)

enum class Beslutning {
    GODKJENT,
    UNDERKJENT;

    fun erGodkjent() = this == GODKJENT
}