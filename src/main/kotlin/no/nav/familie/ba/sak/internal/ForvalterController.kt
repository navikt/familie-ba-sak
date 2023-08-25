package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.RestartAvSmåbarnstilleggService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.concurrent.thread

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val oppgaveRepository: OppgaveRepository,
    private val integrasjonClient: IntegrasjonClient,
    private val restartAvSmåbarnstilleggService: RestartAvSmåbarnstilleggService,
    private val forvalterService: ForvalterService,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

    @PostMapping(
        path = ["/ferdigstill-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun ferdigstillListeMedOppgaver(@RequestBody oppgaveListe: List<Long>): ResponseEntity<String> {
        var antallFeil = 0
        oppgaveListe.forEach { oppgaveId ->
            Result.runCatching {
                ferdigstillOppgave(oppgaveId)
            }.fold(
                onSuccess = { logger.info("Har ferdigstilt oppgave med oppgaveId=$oppgaveId") },
                onFailure = {
                    logger.warn("Klarte ikke å ferdigstille oppgaveId=$oppgaveId", it)
                    antallFeil = antallFeil.inc()
                },
            )
        }
        return ResponseEntity.ok("Ferdigstill oppgaver kjørt. Antall som ikke ble ferdigstilt: $antallFeil")
    }

    @PostMapping(
        path = ["/start-manuell-restart-av-smaabarnstillegg-jobb/skalOppretteOppgaver/{skalOppretteOppgaver}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun triggManuellStartAvSmåbarnstillegg(@PathVariable skalOppretteOppgaver: Boolean = true): ResponseEntity<String> {
        restartAvSmåbarnstilleggService.finnOgOpprettetOppgaveForSmåbarnstilleggSomSkalRestartesIDenneMåned(
            skalOppretteOppgaver,
        )
        return ResponseEntity.ok("OK")
    }

    private fun ferdigstillOppgave(oppgaveId: Long) {
        integrasjonClient.ferdigstillOppgave(oppgaveId)
        oppgaveRepository.findByGsakId(oppgaveId.toString()).also {
            if (it != null && !it.erFerdigstilt) {
                it.erFerdigstilt = true
                oppgaveRepository.saveAndFlush(it)
            }
        }
    }

    @PostMapping(path = ["/lag-og-send-utbetalingsoppdrag-til-økonomi"])
    fun lagOgSendUtbetalingsoppdragTilØkonomi(@RequestBody behandlinger: Set<Long>): ResponseEntity<String> {
        behandlinger.forEach {
            try {
                forvalterService.lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(it)
            } catch (exception: Exception) {
                secureLogger.info(
                    "Kunne ikke sende behandling med id $it til økonomi" +
                        "\n$exception",
                )
            }
        }

        return ResponseEntity.ok("OK")
    }

    @PostMapping("/kjor-satsendring-uten-validering")
    @Transactional
    fun kjørSatsendringFor(@RequestBody fagsakListe: List<Long>) {
        fagsakListe.parallelStream().forEach { fagsakId ->
            try {
                logger.info("Kjører satsendring uten validering for $fagsakId")
                forvalterService.kjørForenkletSatsendringFor(fagsakId)
            } catch (e: Exception) {
                logger.warn("Klarte ikke kjøre satsendring for fagsakId=$fagsakId", e)
            }
        }
    }

    @PostMapping("/identifiser-utbetalinger-over-100-prosent")
    fun identifiserUtbetalingerOver100Prosent(): ResponseEntity<Pair<String, String>> {
        val callId = UUID.randomUUID().toString()
        thread {
            forvalterService.identifiserUtbetalingerOver100Prosent(callId)
        }
        return ResponseEntity.ok(Pair("callId", callId))
    }

    @GetMapping("/finnBehandlingerMedPotensieltFeilUtbetalingsoppdrag")
    fun identifiserBehandlingerSomKanKrevePatching(): ResponseEntity<Pair<String, String>> {
        val callId = UUID.randomUUID().toString()
        thread {
            val behandlingsIder = forvalterService.identifiserBehandlingerSomKanKrevePatching()
            logger.warn("Følgende behandlinger har ikke korrekte opphørsdatoer: [$behandlingsIder]")
        }
        return ResponseEntity.ok(Pair("callId", callId))
    }

    @GetMapping("/sjekkOmTilkjentYtelseForBehandlingHarUkorrektOpphørsdato/{behandlingId}")
    fun sjekkOmTilkjentYtelseForBehandlingHarUkorrektOpphørsdato(@PathVariable behandlingId: Long): ResponseEntity<Boolean> {
        return ResponseEntity.ok(forvalterService.sjekkOmTilkjentYtelseForBehandlingHarUkorrektOpphørsdato(behandlingId))
    }
}
