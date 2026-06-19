package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutobrevScheduler
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.TaBehandlingerEtterVentefristAvVentTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/internal"])
class TestVerktøyController(
    private val scheduler: AutobrevScheduler,
    private val personidentService: PersonidentService,
    private val envService: EnvService,
    private val autovedtakStegService: AutovedtakStegService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilgangService: TilgangService,
    private val simuleringService: SimuleringService,
    private val opprettTaskService: OpprettTaskService,
) {
    @GetMapping(path = ["/autobrev"])
    fun kjørSchedulerForAutobrev(): ResponseEntity<Ressurs<String>> =
        if (envService.erPreprod() || envService.erDev()) {
            scheduler.opprettTask()
            ResponseEntity.ok(Ressurs.success("Laget task."))
        } else {
            ResponseEntity.ok(Ressurs.success(ENDEPUNKTET_GJØR_IKKE_NOE_I_PROD_MELDING))
        }

    @GetMapping(path = ["/test-satsendring/{fagsakId}"])
    fun utførSatsendringPåFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<String>> =
        if (envService.erPreprod() || envService.erDev()) {
            opprettTaskService.opprettSatsendringTask(fagsakId, StartSatsendring.hentAktivSatsendringstidspunkt())
            ResponseEntity.ok(Ressurs.success("Trigget satsendring for fagsak $fagsakId"))
        } else {
            ResponseEntity.ok(Ressurs.success(ENDEPUNKTET_GJØR_IKKE_NOE_I_PROD_MELDING))
        }

    @PostMapping(path = ["/vedtak-om-overgangsstønad"])
    fun mottaHendelseOmVedtakOmOvergangsstønad(
        @RequestBody personIdent: PersonIdent,
    ): ResponseEntity<Ressurs<String>> =
        if (envService.erPreprod() || envService.erDev()) {
            val aktør = personidentService.hentAktør(personIdent.ident)
            val melding =
                autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                    mottakersAktør = aktør,
                    aktør = aktør,
                )
            ResponseEntity.ok(Ressurs.success(melding))
        } else {
            ResponseEntity.ok(Ressurs.success(ENDEPUNKTET_GJØR_IKKE_NOE_I_PROD_MELDING))
        }

    @PostMapping(path = ["/foedselshendelse"])
    fun mottaFødselshendelse(
        @RequestBody nyBehandlingHendelse: NyBehandlingHendelse,
    ): ResponseEntity<Ressurs<String>> =
        if (envService.erPreprod() || envService.erDev()) {
            val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandlingHendelse))
            taskRepository.save(task)
            ResponseEntity.ok(Ressurs.success("Task for behandling av fødselshendelse på ${nyBehandlingHendelse.morsIdent} er opprettet"))
        } else {
            ResponseEntity.ok(Ressurs.success(ENDEPUNKTET_GJØR_IKKE_NOE_I_PROD_MELDING))
        }

    @GetMapping(path = ["/ta-behandlinger-etter-ventefrist-av-vent"])
    fun taBehandlingerEtterVentefristAvVent(): ResponseEntity<Ressurs<String>> =
        if (envService.erPreprod() || envService.erDev()) {
            val taBehandlingerEtterVentefristAvVentTask =
                Task(type = TaBehandlingerEtterVentefristAvVentTask.TASK_STEP_TYPE, payload = "")
            taskRepository.save(taBehandlingerEtterVentefristAvVentTask)
            ResponseEntity.ok(Ressurs.success("Task for å ta behandlinger av vent etter at fristen har gått ut er opprettet"))
        } else {
            ResponseEntity.ok(Ressurs.success(ENDEPUNKTET_GJØR_IKKE_NOE_I_PROD_MELDING))
        }

    @GetMapping(path = ["/hent-simulering-pa-behandling/{behandlingId}"])
    fun hentSimuleringPåBehandling(
        @PathVariable behandlingId: Long,
    ): List<ØkonomiSimuleringMottaker> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)

        return simuleringService.hentSimuleringPåBehandling(behandlingId)
    }

    companion object {
        const val ENDEPUNKTET_GJØR_IKKE_NOE_I_PROD_MELDING = "Endepunktet gjør ingenting i prod."
    }
}
