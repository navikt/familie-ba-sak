package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPeriodeResultat
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.RessursResponse.badRequest
import no.nav.familie.ba.sak.common.RessursResponse.forbidden
import no.nav.familie.ba.sak.common.RessursResponse.illegalState
import no.nav.familie.ba.sak.common.RessursResponse.notFound
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.OpphørVedtakTask
import no.nav.familie.ba.sak.validering.FagsaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
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

    @PutMapping(path = ["/{fagsakId}/vedtak"])
    fun opprettEllerOppdaterVedtak(@PathVariable @FagsaktilgangConstraint fagsakId: Long,
                                   //@RequestBody restPersonResultat: RestPersonResultat): ResponseEntity<Ressurs<RestFagsak>> {
                                   @RequestBody restPersonResultat: RestVilkårsvurdering): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        LOG.info("$saksbehandlerId lager nytt vedtak for fagsak med id $fagsakId")

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching {
                    stegService.håndterVilkårsvurdering(behandling, restPersonResultat)
                }
                .fold(
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId)) },
                        onFailure = {
                            badRequest((it.cause?.message ?: it.message).toString(), null)
                        }
                )
    }

    @PostMapping(path = ["/{fagsakId}/send-til-beslutter"])
    fun sendBehandlingTilBeslutter(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        LOG.info("$saksbehandlerId sender behandling til beslutter for fagsak med id $fagsakId")

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching { stegService.håndterSendTilBeslutter(behandling) }.fold(
                onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId)) },
                onFailure = {
                    illegalState((it.cause?.message ?: it.message).toString(), it)
                }
        )
    }

    @PostMapping(path = ["/{fagsakId}/iverksett-vedtak"])
    fun iverksettVedtak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        return Result.runCatching { stegService.håndterGodkjenneVedtak(behandling) }
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

    @PostMapping(path = ["/{fagsakId}/opphoer-migrert-vedtak"])
    fun opphørMigrertVedtak(@PathVariable @FagsaktilgangConstraint fagsakId: Long): ResponseEntity<Ressurs<String>> {
        val førsteNesteMåned = LocalDate.now().førsteDagINesteMåned()
        return opphørMigrertVedtak(fagsakId,
                                   Opphørsvedtak(førsteNesteMåned))
    }

    @PostMapping(path = ["/{fagsakId}/opphoer-migrert-vedtak/v2"])
    fun opphørMigrertVedtak(@PathVariable @FagsaktilgangConstraint fagsakId: Long, @RequestBody
    opphørsvedtak: Opphørsvedtak): ResponseEntity<Ressurs<String>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        LOG.info("$saksbehandlerId oppretter task for opphør av migrert vedtak for fagsak med id $fagsakId")

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                     ?: return notFound("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        if (behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD) {
            return forbidden("Prøver å opphøre et vedtak for behandling ${behandling.id}, som ikke er migrering")
        }

        if (behandling.status != BehandlingStatus.IVERKSATT && behandling.status != BehandlingStatus.FERDIGSTILT) {
            return forbidden("Prøver å opphøre et vedtak for behandling ${behandling.id}, som ikke er iverksatt/ferdigstilt")
        }

        val task = OpphørVedtakTask.opprettOpphørVedtakTask(behandling,
                                                            vedtak,
                                                            saksbehandlerId,
                                                            BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
                                                            opphørsvedtak.opphørsdato)
        taskRepository.save(task)

        return ResponseEntity.ok(Ressurs.success("Task for opphør av migrert behandling og vedtak på fagsak $fagsakId opprettet"))
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}

data class RestVilkårsvurdering(
        val periodeResultater: List<RestPeriodeResultat>,
        val begrunnelse: String
)
/*
*/

data class Opphørsvedtak(
        val opphørsdato: LocalDate
)