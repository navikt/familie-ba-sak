package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.MånedligValutajusteringScheduler
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.SlettKompetanserTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@RestController
@RequestMapping("/api/forvalter/valuta")
class ForvalterValutaController(
    private val autovedtakMånedligValutajusteringService: AutovedtakMånedligValutajusteringService,
    private val månedligValutajusteringScheduler: MånedligValutajusteringScheduler,
    private val ecbService: ECBService,
    private val featureToggleService: FeatureToggleService,
    private val tilgangService: TilgangService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
) {
    @GetMapping("/hentValutakurs/")
    fun hentValutakursFraEcb(
        @RequestParam valuta: String,
        @RequestParam dato: LocalDate,
    ): ResponseEntity<BigDecimal> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hent valutakurs fra ECB",
        )

        if (!valuta.matches(Regex("[A-Z]{3}"))) {
            throw Feil("Valutakode må ha store bokstaver og være tre bokstaver lang")
        }
        return ResponseEntity.ok(ecbService.hentValutakurs(valuta, dato))
    }

    @PostMapping("/valutajustering/{fagsakId}/juster-valuta")
    @Operation(summary = "Start valutajustering på fagsak for gjeldende måned")
    fun justerValuta(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<MinimalFagsakDto>> {
        val erPersonMedTilgangTilÅStarteValutajustering = featureToggleService.isEnabled(FeatureToggle.KAN_KJØRE_AUTOMATISK_VALUTAJUSTERING_FOR_ENKELT_SAK)

        if (erPersonMedTilgangTilÅStarteValutajustering) {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(fagsakId = fagsakId, måned = YearMonth.now())
        } else {
            throw Feil("Du har ikke tilgang til å kjøre valutajustering")
        }

        val fagsak = fagsakService.hentMinimalFagsakDto(fagsakId)
        return ResponseEntity.ok(fagsak)
    }

    @PostMapping("/start-valutajustering-scheduler")
    @Operation(summary = "Start valutajustering for alle sekundærlandsaker i gjeldende måned")
    fun lagMånedligValutajusteringTask(): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Start valutajustering for alle sekundærlandsaker i gjeldende måned",
        )

        månedligValutajusteringScheduler.lagMånedligValutajusteringTask(triggerTid = LocalDateTime.now())
        return ResponseEntity.ok(Ressurs.success("Valutajustering for alle sekundærlandsaker i gjeldende måned startet"))
    }

    @DeleteMapping("/slett-alle-kompetanser-for-behandling/{behandlingId}")
    @Operation(summary = "Slett kompetanser, utenlandsk periodebeløp og valutakurser for en behandling som er på vilkårsvurderingssteget.")
    fun slettKompetanser(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Slett kompetanser, utenlandsk periodebeløp og valutakurser for en behandling som er på vilkårsvurderingssteget.",
        )

        val task = taskService.save(SlettKompetanserTask.opprettTask(behandlingId))
        return ResponseEntity.ok(Ressurs.success("Kompetanser slettes i task ${task.id}"))
    }
}
