package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.HenleggBehandlingTask
import no.nav.familie.ba.sak.task.HenleggBehandlingTaskDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

const val SATSENDRING = "Satsendring"

@RestController
@RequestMapping("/api/satsendring")
@ProtectedWithClaims(issuer = "azuread")
class SatsendringController(
    private val startSatsendring: StartSatsendring,
    private val tilgangService: TilgangService,
    private val taskRepository: TaskRepositoryWrapper
) {
    @GetMapping(path = ["/kjorsatsendring/{fagsakId}"])
    fun utførSatsendringITaskPåFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        startSatsendring.opprettSatsendringForFagsak(fagsakId)
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for fagsak $fagsakId"))
    }

    @PutMapping(path = ["/{fagsakId}/kjor-satsendring-synkront"])
    fun utførSatsendringSynkrontPåFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<Unit>> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            handling = "Valider vi kan kjøre satsendring",
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER
        )

        val erSatsendringOpprettetOk = startSatsendring.opprettSatsendringSynkrontVedGammelSats(fagsakId)
        return ResponseEntity.ok(if (erSatsendringOpprettetOk) Ressurs.success(Unit) else Ressurs.failure())
    }

    @GetMapping(path = ["/{fagsakId}/kan-kjore-satsendring"])
    fun kanKjøreSatsendringPåFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<Boolean>> {
        return ResponseEntity.ok(Ressurs.success(startSatsendring.kanStarteSatsendringPåFagsak(fagsakId)))
    }

    @PostMapping(path = ["/kjorsatsendringForListeMedIdenter"])
    fun utførSatsendringPåListeIdenter(@RequestBody listeMedIdenter: Set<String>): ResponseEntity<Ressurs<String>> {
        listeMedIdenter.forEach {
            startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(it)
        }
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for liste med identer ${listeMedIdenter.size}"))
    }

    @PostMapping(path = ["/henleggBehandlingerMedLangLiggetid"])
    fun henleggBehandlingerMedLangLiggetid(@RequestBody behandlinger: Set<String>): ResponseEntity<Ressurs<String>> {
        behandlinger.forEach {
            taskRepository.save(
                HenleggBehandlingTask.opprettTask(
                    HenleggBehandlingTaskDTO(
                        behandlingId = it.toLong(),
                        årsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                        begrunnelse = SATSENDRING,
                        validerOppgavefristErEtterDato = LocalDate.of(2023, 4, 1)
                    )
                )
            )
        }
        return ResponseEntity.ok(Ressurs.Companion.success("Trigget henleggelse for ${behandlinger.size} behandlinger"))
    }
}
