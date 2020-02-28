package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakController
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.common.RessursResponse
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdrag
import no.nav.familie.ba.sak.task.OpphørVedtakTask
import no.nav.familie.ba.sak.validering.FagsaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/fagsak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(
        private val behandlingService: BehandlingService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val vedtakService: VedtakService,
        private val fagsakService: FagsakService,
        private val taskRepository: TaskRepository
) {

    @PostMapping(path = ["/{fagsakId}/nytt-vedtak"])
    fun nyttVedtak(@PathVariable @FagsaktilgangConstraint fagsakId: Long,
                   @RequestBody nyttVedtak: NyttVedtak): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        FagsakController.logger.info("{} lager nytt vedtak for fagsak med id {}", saksbehandlerId, fagsakId)

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return RessursResponse.notFound("Fant ikke behandling på fagsak $fagsakId")

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: return RessursResponse.notFound(
                                               "Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        return Result.runCatching {
            vedtakService.nyttVedtakForAktivBehandling(behandling,
                                                       personopplysningGrunnlag,
                                                       nyttVedtak,
                                                       ansvarligSaksbehandler = saksbehandlerId)
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(it) },
                        onFailure = { e ->
                            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Ressurs.failure(e.cause?.message ?: e.message, e))
                        }
                )
    }

    @PostMapping(path = ["/{fagsakId}/send-til-beslutter"])
    fun sendBehandlingTilBeslutter(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        FagsakController.logger.info("{} sender behandling til beslutter for fagsak med id {}", saksbehandlerId, fagsakId)

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return RessursResponse.notFound("Fant ikke behandling på fagsak $fagsakId")

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.SENDT_TIL_BESLUTTER)

        return Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }.fold(
                onSuccess = { ResponseEntity.ok(it) },
                onFailure = { e ->
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Ressurs.failure(e.cause?.message ?: e.message, e))
                }
        )
    }

    @PostMapping(path = ["/{fagsakId}/iverksett-vedtak"])
    fun iverksettVedtak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return RessursResponse.notFound("Fant ikke behandling på fagsak $fagsakId")

        if (behandling.status != BehandlingStatus.SENDT_TIL_BESLUTTER) {
            return RessursResponse.forbidden("Kan ikke iverksette et vedtak som ikke er foreslått av en saksbehandler")
        }

        if (behandling.status == BehandlingStatus.LAGT_PA_KO_FOR_SENDING_MOT_OPPDRAG
            || behandling.status == BehandlingStatus.SENDT_TIL_IVERKSETTING) {
            return RessursResponse.badRequest("Behandlingen er allerede sendt til oppdrag og venter på kvittering")
        } else if (behandling.status == BehandlingStatus.IVERKSATT || behandling.status == BehandlingStatus.FERDIGSTILT) {
            return RessursResponse.badRequest("Behandlingen er allerede iverksatt/ferdigstilt")
        }

        FagsakController.logger.info("{} oppretter task for iverksetting av vedtak for fagsak med id {}",
                                     saksbehandlerId,
                                     fagsakId)

        return Result.runCatching { behandlingService.valider2trinnVedIverksetting(behandling, saksbehandlerId) }
                .fold(
                        onSuccess = {
                            val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                                         ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

                            opprettTaskIverksettMotOppdrag(behandling, vedtak, saksbehandlerId)

                            return Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }.fold(
                                    onSuccess = { ResponseEntity.ok(it) },
                                    onFailure = {
                                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Ressurs.failure(it.cause?.message ?: it.message, it))
                                    }
                            )
                        },
                        onFailure = {
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Ressurs.failure(it.cause?.message ?: it.message, it))
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

        FagsakController.logger.info("{} oppretter task for opphør av migrert vedtak for fagsak med id {}",
                                     saksbehandlerId,
                                     fagsakId)

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
                         ?: return RessursResponse.notFound("Fant ikke behandling på fagsak $fagsakId")

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                     ?: return RessursResponse.notFound("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        if (behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD) {
            return RessursResponse.forbidden("Prøver å opphøre et vedtak for behandling ${behandling.id}, som ikke er migrering")
        }

        if (behandling.status != BehandlingStatus.IVERKSATT && behandling.status != BehandlingStatus.FERDIGSTILT) {
            return RessursResponse.forbidden(
                    "Prøver å opphøre et vedtak for behandling ${behandling.id}, som ikke er iverksatt/ferdigstilt")
        }

        val task = OpphørVedtakTask.opprettOpphørVedtakTask(behandling,
                                                            vedtak,
                                                            saksbehandlerId,
                                                            BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
                                                            opphørsvedtak.opphørsdato)
        taskRepository.save(task)

        return ResponseEntity.ok(Ressurs.success("Task for opphør av migrert behandling og vedtak på fagsak $fagsakId opprettet"))
    }

    private fun opprettTaskIverksettMotOppdrag(behandling: Behandling, vedtak: Vedtak, saksbehandlerId: String) {
        val task = IverksettMotOppdrag.opprettTask(behandling, vedtak, saksbehandlerId)
        taskRepository.save(task)
    }
}